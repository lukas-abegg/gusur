package com.gusur.app.util

import com.gusur.app.model.Event
import com.gusur.app.model.LegacyInfusion
import com.gusur.app.model.Place
import com.gusur.app.model.deriveSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

private val tolerantJson = Json { ignoreUnknownKeys = true; isLenient = true }

actual suspend fun loadScraperFiles(): Pair<List<Place>, List<Event>>? = withContext(Dispatchers.IO) {
    // The desktop app's CWD is the project root (where gradle :composeApp:run is invoked from),
    // so the scraper directory is at ../scraper/ relative to that. We try a couple of likely
    // working-directory layouts so this works regardless of how the app is launched.
    val candidates = listOf(
        File("../scraper"),
        File("scraper"),
        File("../../scraper")
    )
    val scraperDir = candidates.firstOrNull { File(it, "places.json").exists() }
    if (scraperDir == null) {
        println("ScraperLoad: no places.json found in ${candidates.joinToString { it.absolutePath }}")
        return@withContext null
    }
    val placesFile = File(scraperDir, "places.json")
    val eventsFile = File(scraperDir, "events.json")
    val legacyFile = File(scraperDir, "infusion_import.json")

    try {
        if (placesFile.exists() && eventsFile.exists()) {
            val places = tolerantJson.decodeFromString<List<Place>>(placesFile.readText())
            val events = tolerantJson.decodeFromString<List<Event>>(eventsFile.readText())
            println("ScraperLoad: read ${places.size} places + ${events.size} events from ${scraperDir.absolutePath}")
            return@withContext places to events
        }
        if (legacyFile.exists()) {
            val legacy = tolerantJson.decodeFromString<List<LegacyInfusion>>(legacyFile.readText())
            val derived = deriveSchema(legacy)
            println("ScraperLoad: read ${legacy.size} legacy infusions, derived ${derived.first.size} places + ${derived.second.size} events")
            return@withContext derived
        }
        println("ScraperLoad: no scraper files at ${scraperDir.absolutePath}")
        null
    } catch (e: Exception) {
        println("ScraperLoad: failed to parse scraper output: ${e::class.simpleName}: ${e.message}")
        null
    }
}
