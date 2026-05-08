package com.gusur.app.api

import com.gusur.app.model.Event
import com.gusur.app.model.Place
import kotlinx.coroutines.flow.MutableStateFlow

class SaunaAIService(private val apiKey: String) {
    val aiResponse = MutableStateFlow<String>("")
    val isLoading = MutableStateFlow(false)

    suspend fun askAssistant(userQuestion: String, currentShows: List<Event>, placesById: Map<String, Place>) {
        isLoading.value = true
        val context = currentShows.joinToString("\n") { event ->
            val place = placesById[event.place_id]
            "${event.name} at ${place?.name ?: event.place_id}: ${event.description}. Hygiene: ${place?.hygiene_info ?: ""}"
        }

        val prompt = """
            You are 'Gusur AI', an Icelandic Saunagus expert.
            User question: $userQuestion
            Available shows today:
            $context

            Answer concisely and helpfully.
        """.trimIndent()

        // val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
        // val response = model.generateContent(prompt)
        // aiResponse.value = response.text ?: "Sorry, I couldn't process that."

        aiResponse.value = "Based on your request, I recommend the 'Nordic Zen' show at Vesturbæjarlaug. It's focused on relaxation and is currently highly rated!"
        isLoading.value = false
    }
}
