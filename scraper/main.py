"""
Gusur scraper — produces places.json + events.json.

Real bits scraped from https://reykjavik.is/sundlaugar:
  - Pool list (names + URL slugs)
  - Per-pool address + phone (best-effort, falls back to a hardcoded address if parse fails)

Hardcoded (no public structured source):
  - GPS coordinates (Reykjavík city site doesn't publish them)
  - Hygiene info, amenities (would need per-pool deep parse)
  - Premium spas (Sky Lagoon, Laugarvatn Fontana — not city-run)
  - Saunagus event schedules — most pools announce these on Facebook only.
    We generate synthetic upcoming evening sessions so the app has something to show.

When the city site redesigns, the scrape selectors below will break — the function
returns the hardcoded baseline, so the app keeps working. Update SELECTORS as needed.
"""
import argparse
import json
import re
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Optional

import requests
from bs4 import BeautifulSoup

INDEX_URL = "https://reykjavik.is/sundlaugar"
SITE_ROOT = "https://reykjavik.is"
USER_AGENT = "GusurScraper/0.1 (contact: lukas@taskbase.com)"
REQUEST_TIMEOUT = 15

# GPS coordinates aren't published on reykjavik.is — keep a slug→coords lookup.
# Slug is the URL path component (e.g. "vesturbaejarlaug" for /vesturbaejarlaug).
COORDS_BY_SLUG = {
    "vesturbaejarlaug": (64.1472, -21.9424),
    "sundholl-reykjavikur": (64.1418, -21.9224),
    "laugardalslaug": (64.1436, -21.8700),
    "arbaejarlaug": (64.1214, -21.8055),
    "breidholtslaug": (64.1042, -21.8514),
    "dalslaug": (64.1264, -21.7614),
    "grafarvogslaug": (64.1330, -21.8030),
    "klebergslaug": (64.2060, -21.7950),
}

# Saunagus availability per pool. Pools without sessions still appear as Places
# but won't have generated events.
HAS_SAUNAGUS = {
    "vesturbaejarlaug": True,
    "sundholl-reykjavikur": True,
    "laugardalslaug": True,
    "arbaejarlaug": True,
    "breidholtslaug": True,
    "dalslaug": True,
    "grafarvogslaug": False,
    "klebergslaug": False,
}

PREMIUM_SPAS = [
    {
        "id": "Sky Lagoon",
        "name": "Sky Lagoon",
        "lat": 64.1235,
        "lon": -21.9372,
        "address": "Vesturvör 44-48, 200 Kópavogur",
        "description": "Geothermal lagoon with a signature 7-step ritual and ocean views.",
        "hygiene_info": "Luxury facilities, towels provided.",
        "amenities": ["7-step ritual", "Steam room", "Cold plunge", "Sauna", "Sea view"],
        "default_price": 9900.0,
        "ritual_name": "7-Step Ritual",
        "style": "Luxury",
        "length_minutes": 60,
    },
    {
        "id": "Laugarvatn Fontana",
        "name": "Laugarvatn Fontana",
        "lat": 64.2151,
        "lon": -20.7303,
        "address": "Hverabraut 1, 840 Laugarvatn",
        "description": "Lakeside spa using natural geothermal steam baths.",
        "hygiene_info": "Luxury facilities, towels provided.",
        "amenities": ["Geothermal steam bath", "Lake access", "Sauna"],
        "default_price": 4900.0,
        "ritual_name": "Steam Bath Gus",
        "style": "Steam",
        "length_minutes": 45,
    },
]

DEFAULT_HYGIENE = "Shower without swimsuit before entry."
DEFAULT_AMENITIES = ["Hot tubs", "Steam room"]

ICELANDIC_POSTCODE_RE = re.compile(r"\b(\d{3})\s+([A-ZÁÉÍÓÚÝÞÆÖa-záéíóúýþæö][\w\sáéíóúýþæöÁÉÍÓÚÝÞÆÖ-]+)")


def now_ms() -> int:
    return int(time.time() * 1000)


def http_get(url: str, cache_dir: Optional[Path]) -> Optional[str]:
    if cache_dir is not None:
        cache_dir.mkdir(parents=True, exist_ok=True)
        cache_path = cache_dir / (re.sub(r"[^A-Za-z0-9-]", "_", url) + ".html")
        if cache_path.exists():
            return cache_path.read_text(encoding="utf-8")
    try:
        resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=REQUEST_TIMEOUT)
        resp.raise_for_status()
    except requests.RequestException as e:
        print(f"  HTTP error for {url}: {e}")
        return None
    if cache_dir is not None:
        cache_path = cache_dir / (re.sub(r"[^A-Za-z0-9-]", "_", url) + ".html")
        cache_path.write_text(resp.text, encoding="utf-8")
    return resp.text


def fetch_pool_index(cache_dir: Optional[Path]) -> list[dict]:
    """Returns [{slug, name, url}] for each pool found on the index page."""
    html = http_get(INDEX_URL, cache_dir)
    if html is None:
        return []
    soup = BeautifulSoup(html, "html.parser")
    pools: dict[str, dict] = {}
    # Pool entries are anchors like <a href="/<slug>"> wrapping image + name in li.
    for a in soup.select("ul li a[href]"):
        href = a.get("href", "").strip()
        if not href.startswith("/") or "/" in href[1:]:
            continue
        slug = href.lstrip("/")
        if not slug or slug in pools:
            continue
        # Reject non-pool links. Must match a known pool slug (canonical names live in
        # _name_for_slug). Anchor text is unreliable: it bundles opening-hours metadata
        # right after the name, so we don't try to parse the name out of HTML — the slug
        # is the stable identifier.
        if slug not in COORDS_BY_SLUG and not any(t in slug for t in ("laug", "sundholl")):
            continue
        pools[slug] = {"slug": slug, "name": _name_for_slug(slug), "url": SITE_ROOT + href}
    return list(pools.values())


def fetch_pool_detail(pool: dict, cache_dir: Optional[Path]) -> dict:
    """Returns {address, phone} for a single pool, best-effort. Missing fields → empty."""
    html = http_get(pool["url"], cache_dir)
    if html is None:
        return {}
    soup = BeautifulSoup(html, "html.parser")
    text = soup.get_text("\n", strip=True)
    out: dict = {}

    # The reykjavik.is layout typically renders the street as one line and the
    # "<postcode> Reykjavík" piece as the next. We grab the first postcode match
    # (header/top-of-page is the pool address; later matches are city-hall footers).
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    for i, line in enumerate(lines):
        if not ICELANDIC_POSTCODE_RE.search(line):
            continue
        # Skip lines that are clearly the city-hall footer ("Borgartún", "Tjarnargata").
        if any(footer in line for footer in ("Borgartún", "Tjarnargata")):
            continue
        if "," in line:
            out["address"] = line
        elif i > 0 and not ICELANDIC_POSTCODE_RE.search(lines[i - 1]):
            out["address"] = f"{lines[i - 1]}, {line}"
        else:
            out["address"] = line
        break

    phone_m = re.search(r"\b\d{3}\s?\d{4}\b", text)
    if phone_m:
        out["phone"] = phone_m.group(0)
    return out


def build_places(use_scraper: bool, cache_dir: Optional[Path]) -> tuple[list[dict], list[dict]]:
    """Returns (places, pool_meta_with_slug). Spas are included in places only."""
    pools_meta: list[dict] = []
    if use_scraper:
        scraped = fetch_pool_index(cache_dir)
        if scraped:
            print(f"Scraped {len(scraped)} pools from {INDEX_URL}")
            pools_meta = scraped
        else:
            print("Index scrape returned 0 pools — falling back to hardcoded slugs.")
    if not pools_meta:
        pools_meta = [
            {"slug": slug, "name": _name_for_slug(slug), "url": f"{SITE_ROOT}/{slug}"}
            for slug in COORDS_BY_SLUG
        ]

    places = []
    valid_pool_meta = []
    created_at = now_ms()
    for meta in pools_meta:
        slug = meta["slug"]
        coords = COORDS_BY_SLUG.get(slug)
        if not coords:
            print(f"  No coordinates known for slug {slug!r}; skipping.")
            continue

        details = fetch_pool_detail(meta, cache_dir) if use_scraper else {}
        places.append({
            "id": meta["name"],
            "name": meta["name"],
            "address": details.get("address", f"{meta['name']}, Reykjavík"),
            "latitude": coords[0],
            "longitude": coords[1],
            "description": "City pool with regular Saunagus sessions." if HAS_SAUNAGUS.get(slug) else "City pool.",
            "image_url": None,
            "hygiene_info": DEFAULT_HYGIENE,
            "amenities": DEFAULT_AMENITIES,
            "average_rating": None,
            "num_reviews": 0,
            "maintainer_user_id": None,
            "created_by": "scraper",
            "created_at": created_at,
        })
        valid_pool_meta.append(meta)

    for spa in PREMIUM_SPAS:
        places.append({
            "id": spa["id"],
            "name": spa["name"],
            "address": spa["address"],
            "latitude": spa["lat"],
            "longitude": spa["lon"],
            "description": spa["description"],
            "image_url": None,
            "hygiene_info": spa["hygiene_info"],
            "amenities": spa["amenities"],
            "average_rating": None,
            "num_reviews": 0,
            "maintainer_user_id": None,
            "created_by": "scraper",
            "created_at": created_at,
        })
    return places, valid_pool_meta


def _name_for_slug(slug: str) -> str:
    """Best-guess pretty name for a slug when the index scrape failed."""
    overrides = {
        "vesturbaejarlaug": "Vesturbæjarlaug",
        "sundholl-reykjavikur": "Sundhöll Reykjavíkur",
        "laugardalslaug": "Laugardalslaug",
        "arbaejarlaug": "Árbæjarlaug",
        "breidholtslaug": "Breiðholtslaug",
        "dalslaug": "Dalslaug",
        "grafarvogslaug": "Grafarvogslaug",
        "klebergslaug": "Klébergslaug",
    }
    return overrides.get(slug, slug.replace("-", " ").title())


def _synthetic_pool_events(meta: dict) -> list[dict]:
    today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    out: list[dict] = []
    for day in range(1, 8):
        session_date = today + timedelta(days=day)
        for hour in (18, 19, 20):
            session = session_date.replace(hour=hour)
            session_id = session.strftime("%Y%m%dT%H%M")
            out.append({
                # Deterministic id so re-runs upsert instead of creating duplicates.
                "id": f"synth-{meta['slug']}-{session_id}",
                "place_id": meta["name"],
                "name": f"Saunagus - {meta['name']}",
                "datetime": session.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "price": 1330.0,
                "currency": "ISK",
                "requirements": ["Towel", "Swimsuit"],
                "is_recurring": True,
                "description": f"Guided Saunagus session at {meta['name']}. Arrive 10 min early.",
                "image_url": None,
                "heat_level": 90,
                "length_minutes": 15,
                "scents": [],
                "style": "Traditional",
                "sauna_meister": "Gus Master",
                "created_by": "scraper:synthetic",
            })
    return out


def _fontana_events(tour_times: list[str]) -> list[dict]:
    """Real recurring bakery-tour events at Laugarvatn Fontana, scraped from fontana.is."""
    out: list[dict] = []
    today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    for day in range(1, 8):
        for time_str in tour_times:
            hour, minute = (int(x) for x in time_str.split(":"))
            session = (today + timedelta(days=day)).replace(hour=hour, minute=minute)
            # Deterministic id per (place, datetime) so re-runs upsert.
            session_id = session.strftime("%Y%m%dT%H%M")
            out.append({
                "id": f"fontana-tour-{session_id}",
                "place_id": "Laugarvatn Fontana",
                "name": "Geothermal Bakery Tour",
                "datetime": session.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "price": 0.0,  # included with admission
                "currency": "ISK",
                "requirements": [],
                "is_recurring": True,
                "description": "Watch rye bread baked in geothermal sand at the lake's edge.",
                "image_url": None,
                "heat_level": 0,
                "length_minutes": 30,
                "scents": [],
                "style": "Tour",
                "sauna_meister": "Tour Guide",
                "created_by": "scraper:fontana",
            })
    return out


def build_events(pool_meta: list[dict], cache_dir: Optional[Path]) -> list[dict]:
    events: list[dict] = []

    # 1) Curated YAML schedule (real data when present). Each place mentioned
    #    in saunagus_schedule.yaml is "covered" — its synthetic placeholders are
    #    skipped below.
    try:
        from schedule import load_curated_schedule
        curated_events, curated_places = load_curated_schedule()
        if curated_events:
            print(f"  Curated schedule: {len(curated_events)} events across {len(curated_places)} places")
        events.extend(curated_events)
    except Exception as e:
        print(f"  Curated schedule load failed: {e}")
        curated_places = set()

    # 2) Synthetic Saunagus placeholders for city pools NOT covered by the
    #    curated schedule. Real schedules aren't published structurally
    #    (Facebook only); curate above to replace these one place at a time.
    for meta in pool_meta:
        if meta["name"] in curated_places:
            continue
        if HAS_SAUNAGUS.get(meta["slug"], False):
            events.extend(_synthetic_pool_events(meta))

    # 3) Real bakery-tour events from Fontana (scraped from fontana.is).
    if "Laugarvatn Fontana" not in curated_places:
        try:
            from fontana import fetch_fontana_status
            status = fetch_fontana_status(cache_dir)
            print(f"  Fontana: tour_times={status['tour_times']} baths_closed={status['baths_closed']}")
            events.extend(_fontana_events(status["tour_times"]))
        except Exception as e:
            print(f"  Fontana scrape failed: {e}")

    # 4) One synthetic Sky Lagoon "ritual" entry per day for the next 3 days.
    if "Sky Lagoon" not in curated_places:
        today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
        for day in range(1, 4):
            session = (today + timedelta(days=day)).replace(hour=14)
            events.append({
                "id": f"sky-ritual-{session.strftime('%Y%m%d')}",
                "place_id": "Sky Lagoon",
                "name": "7-Step Ritual",
                "datetime": session.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "price": 9900.0,
                "currency": "ISK",
                "requirements": ["Swimsuit"],
                "is_recurring": True,
                "description": "Premium experience at Sky Lagoon.",
                "image_url": None,
                "heat_level": 80,
                "length_minutes": 60,
                "scents": [],
                "style": "Luxury",
                "sauna_meister": "Spa Master",
                "created_by": "scraper:synthetic",
            })

    return events


def parse_args():
    p = argparse.ArgumentParser(description="Gusur scraper — emits places.json + events.json.")
    p.add_argument("--no-scrape", action="store_true", help="Skip live scraping; use hardcoded baseline only.")
    p.add_argument("--cache-dir", type=Path, default=Path(__file__).parent / ".http_cache",
                   help="Directory for cached HTTP responses (default: ./.http_cache). Use '' to disable.")
    return p.parse_args()


def main() -> None:
    args = parse_args()
    cache_dir: Optional[Path] = args.cache_dir if str(args.cache_dir) else None
    use_scraper = not args.no_scrape

    places, pool_meta = build_places(use_scraper=use_scraper, cache_dir=cache_dir)
    events = build_events(pool_meta, cache_dir)

    out_dir = Path(__file__).parent
    (out_dir / "places.json").write_text(
        json.dumps(places, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    (out_dir / "events.json").write_text(
        json.dumps(events, indent=2, ensure_ascii=False), encoding="utf-8"
    )
    print(f"Wrote {len(places)} places to places.json and {len(events)} events to events.json")


if __name__ == "__main__":
    main()