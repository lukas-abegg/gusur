package com.gusur.app.util

import com.gusur.app.model.Event
import com.gusur.app.model.LegacyInfusion
import com.gusur.app.model.Place
import com.gusur.app.model.deriveSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Periodically polls the scraper output directory for updates on Desktop.
 *
 * Preferred input: `../scraper/places.json` + `../scraper/events.json`.
 * Backwards-compatible: if those don't exist but `../scraper/infusion_import.json` does,
 * it's parsed as the legacy schema and converted via deriveSchema.
 */
suspend fun watchScraperFile(onUpdate: (List<Place>, List<Event>) -> Unit) {
    val placesFile = File("../scraper/places.json")
    val eventsFile = File("../scraper/events.json")
    val legacyFile = File("../scraper/infusion_import.json")

    var lastNewSchemaTimestamp = 0L
    var lastLegacyTimestamp = 0L

    val tolerantJson = Json { ignoreUnknownKeys = true; isLenient = true }

    while (true) {
        try {
            val newSchemaPresent = placesFile.exists() && eventsFile.exists()
            if (newSchemaPresent) {
                val combinedTs = maxOf(placesFile.lastModified(), eventsFile.lastModified())
                if (combinedTs > lastNewSchemaTimestamp) {
                    lastNewSchemaTimestamp = combinedTs
                    val placesText = withContext(Dispatchers.IO) { placesFile.readText() }
                    val eventsText = withContext(Dispatchers.IO) { eventsFile.readText() }
                    val places = tolerantJson.decodeFromString<List<Place>>(placesText)
                    val events = tolerantJson.decodeFromString<List<Event>>(eventsText)
                    onUpdate(places, events)
                    println("App updated from scraper (${places.size} places, ${events.size} events)")
                }
            } else if (legacyFile.exists() && legacyFile.lastModified() > lastLegacyTimestamp) {
                lastLegacyTimestamp = legacyFile.lastModified()
                val content = withContext(Dispatchers.IO) { legacyFile.readText() }
                val legacy = tolerantJson.decodeFromString<List<LegacyInfusion>>(content)
                val (p, e) = deriveSchema(legacy)
                onUpdate(p, e)
                println("App updated from legacy scraper output (${p.size} places, ${e.size} events)")
            }
        } catch (_: Exception) {
            // ignore partial writes
        }
        delay(2000)
    }
}
