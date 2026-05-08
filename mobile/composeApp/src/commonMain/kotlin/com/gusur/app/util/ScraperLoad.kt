package com.gusur.app.util

import com.gusur.app.model.Event
import com.gusur.app.model.Place

/**
 * One-shot read of scraper output files. Returns null on platforms that don't have
 * filesystem access to ../scraper/ (Android, iOS). Desktop reads places.json + events.json
 * relative to the gradle working directory (the running app's CWD).
 */
expect suspend fun loadScraperFiles(): Pair<List<Place>, List<Event>>?
