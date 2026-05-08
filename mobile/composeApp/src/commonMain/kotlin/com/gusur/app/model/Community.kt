package com.gusur.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Attendance(
    val id: String = "",
    val event_id: String,
    val place_id: String,
    val user_id: String,
    /** "going" | "attended" — "attended" is set when the user checks in at the venue. */
    val status: String = "going",
    val timestamp: Long = 0
)

object AttendanceStatus {
    const val GOING = "going"
    const val ATTENDED = "attended"
}

@Serializable
data class Follow(
    val id: String = "",
    val place_id: String,
    val user_id: String,
    val timestamp: Long = 0
)

@Serializable
data class Report(
    val id: String = "",
    val target_type: String,
    val target_id: String,
    val reason: String,
    val comment: String? = null,
    val user_id: String,
    val user_email: String,
    val timestamp: Long = 0,
    val status: String = "open"
)

object ReportReasons {
    const val CLOSED = "closed"
    const val WRONG_INFO = "wrong_info"
    const val DUPLICATE = "duplicate"
    const val INAPPROPRIATE = "inappropriate"
    const val SUGGEST_EDIT = "suggest_edit"
    const val OTHER = "other"

    val all = listOf(CLOSED, WRONG_INFO, DUPLICATE, INAPPROPRIATE, SUGGEST_EDIT, OTHER)
}

object TargetType {
    const val PLACE = "place"
    const val EVENT = "event"
}

fun attendanceId(eventId: String, userId: String): String = "${eventId}__$userId"
fun followId(placeId: String, userId: String): String = "${placeId}__$userId"

/**
 * Counters derived from authoritative collections (ratings, attendances).
 * Not stored on UserProfile — recomputed on demand so they can't be inflated client-side.
 */
data class UserStats(
    val reviewsWritten: Int = 0,
    val eventsAttended: Int = 0,
    val eventsRSVPd: Int = 0
) {
    companion object {
        val Empty = UserStats()
    }
}

private data class LevelTier(val level: Int, val minReviews: Int, val minAttended: Int)

private val tiers = listOf(
    LevelTier(level = 3, minReviews = 8, minAttended = 25),
    LevelTier(level = 2, minReviews = 3, minAttended = 10),
    LevelTier(level = 1, minReviews = 1, minAttended = 3),
    LevelTier(level = 0, minReviews = 0, minAttended = 0)
)

fun computeLevel(stats: UserStats): Int =
    tiers.first { stats.reviewsWritten >= it.minReviews || stats.eventsAttended >= it.minAttended }.level

fun levelTitle(level: Int): String = when (level) {
    0 -> "Newcomer"
    1 -> "Heat Hunter"
    2 -> "Steam Regular"
    else -> "Sauna Sage"
}

/**
 * Hint string telling the user the cheapest path to the next level, or null at the cap.
 */
fun nextLevelHint(stats: UserStats): String? {
    val current = computeLevel(stats)
    val next = tiers.firstOrNull { it.level == current + 1 } ?: return null
    val reviewsNeeded = (next.minReviews - stats.reviewsWritten).coerceAtLeast(0)
    val attendedNeeded = (next.minAttended - stats.eventsAttended).coerceAtLeast(0)
    val nextTitle = levelTitle(next.level)
    return when {
        reviewsNeeded == 0 || attendedNeeded == 0 -> null
        reviewsNeeded <= attendedNeeded ->
            "$reviewsNeeded more review${if (reviewsNeeded == 1) "" else "s"} to $nextTitle"
        else ->
            "$attendedNeeded more check-in${if (attendedNeeded == 1) "" else "s"} to $nextTitle"
    }
}

fun canEditPlace(profile: UserProfile, place: Place, isAdmin: Boolean): Boolean =
    isAdmin || place.maintainer_user_id == profile.id

fun canSuggestEdit(stats: UserStats): Boolean =
    computeLevel(stats) >= 2
