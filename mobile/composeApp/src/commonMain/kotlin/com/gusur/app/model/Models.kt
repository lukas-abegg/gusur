package com.gusur.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String,
    val name: String,
    val address: String = "",
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val image_url: String? = null,
    val hygiene_info: String = "",
    val amenities: List<String> = emptyList(),
    val average_rating: Double? = null,
    val num_reviews: Int = 0,
    val maintainer_user_id: String? = null,
    val created_by: String? = null,
    val created_at: Long = 0
)

@Serializable
data class Event(
    val id: String,
    val place_id: String,
    val name: String,
    val datetime: String,
    val price: Double,
    val currency: String = "ISK",
    val requirements: List<String> = emptyList(),
    val is_recurring: Boolean = false,
    val description: String? = null,
    val image_url: String? = null,
    val heat_level: Int = 90,
    val length_minutes: Int = 15,
    val scents: List<String> = emptyList(),
    val style: String = "Traditional",
    val sauna_meister: String = "Gus Master",
    val created_by: String? = null
)

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val name: String = "New Hunter",
    val location: String = "Reykjavík",
    val photoUrl: String? = null
)

@Serializable
data class Rating(
    val id: String = "",
    val place_id: String,
    val user_id: String,
    val user_email: String,
    val heat_management: Int,
    val aroma: Int,
    val technique: Int,
    val atmosphere: Int,
    val hygiene: Int,
    val comment: String? = null,
    val timestamp: Long = 0,
    val vibes: List<String> = emptyList()
)

@Serializable
data class LegacyInfusion(
    val id: String,
    val name: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val datetime: String,
    val price: Double,
    val currency: String = "ISK",
    val requirements: List<String> = emptyList(),
    val hygiene_info: String = "",
    val is_recurring: Boolean = false,
    val description: String? = null,
    val image_url: String? = null,
    val average_rating: Double? = null,
    val heat_level: Int = 90,
    val length_minutes: Int = 15,
    val scents: List<String> = emptyList(),
    val style: String = "Traditional",
    val sauna_meister: String = "Gus Master"
)

fun deriveSchema(legacy: List<LegacyInfusion>): Pair<List<Place>, List<Event>> {
    val places = legacy.groupBy { it.location }.map { (location, items) ->
        val first = items.first()
        val ratings = items.mapNotNull { it.average_rating }
        Place(
            id = location,
            name = location,
            address = location,
            latitude = first.latitude,
            longitude = first.longitude,
            description = items.firstNotNullOfOrNull { it.description },
            image_url = items.firstNotNullOfOrNull { it.image_url },
            hygiene_info = items.firstOrNull { it.hygiene_info.isNotBlank() }?.hygiene_info ?: "",
            average_rating = ratings.takeIf { it.isNotEmpty() }?.average()
        )
    }
    val events = legacy.map { l ->
        Event(
            id = l.id,
            place_id = l.location,
            name = l.name,
            datetime = l.datetime,
            price = l.price,
            currency = l.currency,
            requirements = l.requirements,
            is_recurring = l.is_recurring,
            description = l.description,
            image_url = l.image_url,
            heat_level = l.heat_level,
            length_minutes = l.length_minutes,
            scents = l.scents,
            style = l.style,
            sauna_meister = l.sauna_meister
        )
    }
    return places to events
}
