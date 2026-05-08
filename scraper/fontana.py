"""
Laugarvatn Fontana scraper.

The geothermal baths are seasonally closed (currently for renovations until
2026-06-01) but bakery tours run year-round. We hit fontana.is/en for two reasons:
  1. Confirm the operator's site is alive (so we don't ship stale events when
     the venue suddenly closes permanently).
  2. Detect closure language to flag baths_closed in the Place description.

Tour times themselves are published constants on the operator site (11:45 / 14:30
daily, +10:15 summer). We don't try to scrape arbitrary HH:MM substrings — the
HTML contains JS timestamps, schema.org metadata, etc. that the regex can't
distinguish from real tour times. If the operator changes the schedule, update
the constants below.

Booking at book.fontana.is is a stateful HTML calendar with no iCal/JSON feed.
"""
from __future__ import annotations

import re
from datetime import date
from typing import Optional

from main import http_get  # reuse the same UA + cache

FONTANA_URL = "https://www.fontana.is/en"

YEAR_ROUND_TOUR_TIMES = ["11:45", "14:30"]
SUMMER_EXTRA_TIME = "10:15"   # added June 1 – September 30 per operator's site
SUMMER_MONTHS = (6, 7, 8, 9)


def _published_tour_times(today: Optional[date] = None) -> list[str]:
    today = today or date.today()
    if today.month in SUMMER_MONTHS:
        return [SUMMER_EXTRA_TIME, *YEAR_ROUND_TOUR_TIMES]
    return list(YEAR_ROUND_TOUR_TIMES)


def fetch_fontana_status(cache_dir) -> dict:
    """Returns {'tour_times': [...], 'baths_closed': bool, 'closure_note': str|None}.
    If the page is unreachable, falls back to the published times + assumes baths open."""
    html = http_get(FONTANA_URL, cache_dir)
    if html is None:
        return {"tour_times": _published_tour_times(), "baths_closed": False, "closure_note": None}

    text_lower = html.lower()
    baths_closed = any(
        phrase in text_lower
        for phrase in ("closed for renovations", "closed for maintenance", "currently closed")
    )
    closure_note: Optional[str] = None
    m = re.search(r"closed[^.]{0,80}?(?:until|reopen[^.]*?)\s*([A-Z][a-z]+\s+\d+(?:,\s*\d{4})?)", html)
    if m:
        closure_note = f"Currently closed; reopens {m.group(1)}"

    return {
        "tour_times": _published_tour_times(),
        "baths_closed": baths_closed,
        "closure_note": closure_note,
    }
