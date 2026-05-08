"""
Curated Saunagus schedule loader.

Reads scraper/saunagus_schedule.yaml — a hand-edited file the maintainer updates
weekly. Each place mentioned in the YAML gets real events generated from
recurring rules + one-off entries. Places NOT mentioned fall back to synthetic
events in main.py.

Event IDs are deterministic (slug + datetime) so upload.py upserts on re-runs
instead of creating duplicate docs in Firestore.
"""
from __future__ import annotations

from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Optional

try:
    import yaml
except ImportError:
    yaml = None  # type: ignore

SCHEDULE_FILE = Path(__file__).parent / "saunagus_schedule.yaml"
LOOKAHEAD_DAYS = 14
WEEKDAY_INDEX = {"mon": 0, "tue": 1, "wed": 2, "thu": 3, "fri": 4, "sat": 5, "sun": 6}


def load_curated_schedule(path: Path = SCHEDULE_FILE) -> tuple[list[dict], set[str]]:
    """Returns (events, covered_place_ids). Empty/missing/broken → ([], set())."""
    if yaml is None:
        print("  Schedule loader: PyYAML not installed; skipping curated schedule.")
        return [], set()
    if not path.exists():
        return [], set()
    try:
        with path.open("r", encoding="utf-8") as f:
            data = yaml.safe_load(f) or {}
    except yaml.YAMLError as e:
        print(f"  Schedule loader: parse error in {path.name}: {e}")
        return [], set()

    schedules = data.get("schedules") or {}
    if not schedules:
        return [], set()

    today = datetime.now(timezone.utc).replace(hour=0, minute=0, second=0, microsecond=0)
    events: list[dict] = []
    covered: set[str] = set()
    for place_id, spec in schedules.items():
        if not isinstance(spec, dict):
            continue
        place_events = _events_for_place(place_id, spec, today)
        if place_events:
            events.extend(place_events)
            covered.add(place_id)
    return events, covered


def _events_for_place(place_id: str, spec: dict, today: datetime) -> list[dict]:
    out: list[dict] = []
    common = _meta(spec, fallback=None)

    for rule in spec.get("recurring") or []:
        if not isinstance(rule, dict):
            continue
        rule_days = [str(d).lower()[:3] for d in (rule.get("days") or [])]
        rule_times = rule.get("times") or []
        if not rule_days or not rule_times:
            continue
        rule_meta = _meta(rule, fallback=common)
        for day_offset in range(LOOKAHEAD_DAYS):
            session_date = today + timedelta(days=day_offset)
            if session_date.strftime("%a").lower()[:3] not in rule_days:
                continue
            for time_str in rule_times:
                session_dt = _combine(session_date, time_str)
                if session_dt is not None:
                    out.append(_event_dict(place_id, session_dt, rule_meta))

    for one_off in spec.get("one_offs") or []:
        if not isinstance(one_off, dict):
            continue
        date_str = str(one_off.get("date") or "")
        time_str = str(one_off.get("time") or "")
        if not date_str or not time_str:
            continue
        try:
            session_date = datetime.strptime(date_str, "%Y-%m-%d").replace(tzinfo=timezone.utc)
        except ValueError:
            print(f"  Schedule loader: skipping malformed date {date_str!r} for {place_id}")
            continue
        session_dt = _combine(session_date, time_str)
        if session_dt is None:
            continue
        meta = _meta(one_off, fallback=common)
        out.append(_event_dict(place_id, session_dt, meta))

    return out


def _meta(source: dict, fallback: Optional[dict]) -> dict:
    fallback = fallback or {}
    return {
        "description": source.get("description", fallback.get("description")),
        "sauna_meister": source.get("sauna_meister", fallback.get("sauna_meister", "Gus Master")),
        "style": source.get("style", fallback.get("style", "Traditional")),
        "heat_level": source.get("heat_level", fallback.get("heat_level", 90)),
        "length_minutes": source.get("length_minutes", fallback.get("length_minutes", 15)),
        "scents": source.get("scents", fallback.get("scents", [])),
        "price": source.get("price", fallback.get("price", 1330.0)),
        "currency": source.get("currency", fallback.get("currency", "ISK")),
    }


def _combine(date_dt: datetime, time_str: str) -> Optional[datetime]:
    try:
        hour, minute = (int(x) for x in time_str.split(":"))
    except ValueError:
        print(f"  Schedule loader: skipping malformed time {time_str!r}")
        return None
    return date_dt.replace(hour=hour, minute=minute)


def _slugify(text: str) -> str:
    return "".join(c.lower() if c.isalnum() else "-" for c in text).strip("-") or "place"


def _event_dict(place_id: str, session_dt: datetime, meta: dict) -> dict:
    session_id = session_dt.strftime("%Y%m%dT%H%M")
    return {
        "id": f"curated-{_slugify(place_id)}-{session_id}",
        "place_id": place_id,
        "name": f"Saunagus - {place_id}",
        "datetime": session_dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "price": float(meta["price"]),
        "currency": meta["currency"],
        "requirements": ["Towel", "Swimsuit"],
        "is_recurring": True,
        "description": meta["description"] or f"Saunagus session at {place_id}.",
        "image_url": None,
        "heat_level": int(meta["heat_level"]),
        "length_minutes": int(meta["length_minutes"]),
        "scents": list(meta["scents"]),
        "style": meta["style"],
        "sauna_meister": meta["sauna_meister"],
        "created_by": "scraper:curated",
    }
