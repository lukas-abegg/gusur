package com.gusur.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.gusur.app.api.FirebaseService
import com.gusur.app.model.AttendanceStatus
import com.gusur.app.model.Event
import com.gusur.app.model.LegacyInfusion
import com.gusur.app.model.Place
import com.gusur.app.model.Rating
import com.gusur.app.model.Report
import com.gusur.app.model.UserProfile
import com.gusur.app.model.UserStats
import com.gusur.app.model.deriveSchema
import com.gusur.app.util.loadScraperFiles
import gusur.composeapp.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi

class AppState(
    val firebaseService: FirebaseService,
    val coroutineScope: CoroutineScope
) {
    val places: SnapshotStateList<Place> = mutableStateListOf()
    val events: SnapshotStateList<Event> = mutableStateListOf()

    private val placesByIdState = derivedStateOf { places.associateBy { it.id } }
    val placesById: Map<String, Place> get() = placesByIdState.value

    var attendingEventIds: Set<String> by mutableStateOf(emptySet())
        private set
    var attendedEventIds: Set<String> by mutableStateOf(emptySet())
        private set
    var followedPlaceIds: Set<String> by mutableStateOf(emptySet())
        private set
    var myStats: UserStats by mutableStateOf(UserStats.Empty)
        private set

    val userLat: Double = 64.1466
    val userLon: Double = -21.9426

    var loginErrorMessage: String? by mutableStateOf(null)
        private set

    fun placeById(id: String?): Place? = id?.let { placesById[it] }
    fun eventById(id: String?): Event? = id?.let { e -> events.firstOrNull { it.id == e } }

    @OptIn(ExperimentalResourceApi::class)
    fun loadInitialData() {
        coroutineScope.launch {
            val fetchedPlaces = firebaseService.fetchPlaces()
            val fetchedEvents = firebaseService.fetchEvents()
            if (fetchedPlaces.isNotEmpty() || fetchedEvents.isNotEmpty()) {
                places.clear(); places.addAll(fetchedPlaces)
                events.clear(); events.addAll(fetchedEvents)
                println("AppState: loaded ${fetchedPlaces.size} places, ${fetchedEvents.size} events from Firestore")
            } else if (places.isEmpty() && events.isEmpty()) {
                try {
                    val bytes = Res.readBytes("files/infusions.json")
                    val legacy = Json.decodeFromString<List<LegacyInfusion>>(bytes.decodeToString())
                    val (p, e) = deriveSchema(legacy)
                    places.addAll(p); events.addAll(e)
                    println("AppState: loaded ${legacy.size} legacy infusions from bundled seed → ${p.size} places, ${e.size} events")
                } catch (e: Exception) {
                    println("AppState: failed to read bundled seed (files/infusions.json): ${e::class.simpleName}: ${e.message}")
                }
            }

            firebaseService.currentUserProfile.value?.id?.let { uid ->
                val userAttendances = firebaseService.fetchUserAttendances(uid)
                attendingEventIds = userAttendances.filter { it.status == AttendanceStatus.GOING }.map { it.event_id }.toSet()
                attendedEventIds = userAttendances.filter { it.status == AttendanceStatus.ATTENDED }.map { it.event_id }.toSet()
                followedPlaceIds = firebaseService.fetchUserFollows(uid).map { it.place_id }.toSet()
                myStats = firebaseService.fetchUserStats(uid)
            }
        }
    }

    /**
     * Re-reads the user's attendance docs from Firestore and reconciles attendingEventIds,
     * attendedEventIds, and the attendance-derived parts of myStats. Use after any
     * attendance mutation so optimistic state can't drift from the source of truth.
     */
    private fun reconcileAttendances() {
        val uid = firebaseService.currentUserProfile.value?.id ?: return
        coroutineScope.launch {
            val userAttendances = firebaseService.fetchUserAttendances(uid)
            attendingEventIds = userAttendances.filter { it.status == AttendanceStatus.GOING }.map { it.event_id }.toSet()
            attendedEventIds = userAttendances.filter { it.status == AttendanceStatus.ATTENDED }.map { it.event_id }.toSet()
            myStats = myStats.copy(
                eventsAttended = userAttendances.count { it.status == AttendanceStatus.ATTENDED },
                eventsRSVPd = userAttendances.size
            )
        }
    }

    private fun reconcileFollows() {
        val uid = firebaseService.currentUserProfile.value?.id ?: return
        coroutineScope.launch {
            followedPlaceIds = firebaseService.fetchUserFollows(uid).map { it.place_id }.toSet()
        }
    }

    fun login(email: String, password: String) {
        coroutineScope.launch {
            val (success, msg) = firebaseService.login(email, password)
            loginErrorMessage = if (!success) msg else null
        }
    }

    fun register(email: String, password: String, name: String) {
        coroutineScope.launch {
            val (success, msg) = firebaseService.register(email, password, name)
            loginErrorMessage = if (!success) msg else null
        }
    }

    fun saveUserProfile(profile: UserProfile) {
        coroutineScope.launch { firebaseService.saveUserProfile(profile) }
    }

    fun submitReview(placeId: String, userId: String, userEmail: String, heat: Int, comment: String) {
        coroutineScope.launch {
            firebaseService.submitRating(
                Rating(
                    place_id = placeId,
                    user_id = userId,
                    user_email = userEmail,
                    heat_management = heat,
                    aroma = 5, technique = 5, atmosphere = 5, hygiene = 5,
                    comment = comment.takeIf { it.isNotBlank() },
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
            )
            myStats = firebaseService.fetchUserStats(userId)
        }
    }

    suspend fun fetchRatings(placeId: String): List<Rating> = firebaseService.fetchRatings(placeId)

    fun syncToFirestore() {
        coroutineScope.launch {
            firebaseService.syncPlaces(places.toList())
            firebaseService.syncEvents(events.toList())
        }
    }

    fun migrateLegacyInfusions() {
        coroutineScope.launch {
            val (legacyCount, pCount, eCount) = firebaseService.migrateLegacyInfusions()
            println("Migrated $legacyCount legacy infusions → $pCount places, $eCount events")
            val refreshedPlaces = firebaseService.fetchPlaces()
            val refreshedEvents = firebaseService.fetchEvents()
            places.clear(); places.addAll(refreshedPlaces)
            events.clear(); events.addAll(refreshedEvents)
        }
    }

    /**
     * Desktop-only: replaces local places/events with the scraper's latest output
     * (../scraper/places.json + ../scraper/events.json). On Android/iOS this is a no-op.
     * After loading, tap "SYNC PLACES & EVENTS" to push to Firestore.
     */
    fun loadFromScraper() {
        coroutineScope.launch {
            val result = loadScraperFiles()
            if (result == null) {
                println("AppState: scraper feed unavailable on this platform (or no scraper files found)")
                return@launch
            }
            val (p, e) = result
            places.clear(); places.addAll(p)
            events.clear(); events.addAll(e)
            println("AppState: loaded ${p.size} places + ${e.size} events from scraper output")
        }
    }

    // --- Community: RSVP, Check-in, Follow, Report, Edit Place ---

    fun isAttending(eventId: String): Boolean = eventId in attendingEventIds
    fun hasAttended(eventId: String): Boolean = eventId in attendedEventIds
    fun isFollowing(placeId: String): Boolean = placeId in followedPlaceIds

    fun toggleAttendance(eventId: String, placeId: String) {
        val profile = firebaseService.currentUserProfile.value ?: return
        if (eventId in attendedEventIds) return // already checked in — no toggle
        val going = eventId !in attendingEventIds
        attendingEventIds = if (going) attendingEventIds + eventId else attendingEventIds - eventId
        coroutineScope.launch {
            firebaseService.setAttendance(eventId, placeId, profile.id, going)
            reconcileAttendances()
        }
    }

    fun checkIn(eventId: String, placeId: String) {
        val profile = firebaseService.currentUserProfile.value ?: return
        // optimistic local update
        attendingEventIds = attendingEventIds - eventId
        attendedEventIds = attendedEventIds + eventId
        coroutineScope.launch {
            firebaseService.checkInToEvent(eventId, placeId, profile.id)
            reconcileAttendances()
        }
    }

    fun toggleFollow(placeId: String) {
        val profile = firebaseService.currentUserProfile.value ?: return
        val following = placeId !in followedPlaceIds
        followedPlaceIds = if (following) followedPlaceIds + placeId else followedPlaceIds - placeId
        coroutineScope.launch {
            firebaseService.setFollow(placeId, profile.id, following)
            reconcileFollows()
        }
    }

    suspend fun fetchEventAttendees(eventId: String): Int =
        firebaseService.fetchEventAttendees(eventId).size

    suspend fun fetchPlaceFollowerCount(placeId: String): Int =
        firebaseService.fetchPlaceFollowers(placeId).size

    fun submitReport(targetType: String, targetId: String, reason: String, comment: String) {
        val profile = firebaseService.currentUserProfile.value ?: return
        coroutineScope.launch {
            firebaseService.submitReport(
                Report(
                    target_type = targetType,
                    target_id = targetId,
                    reason = reason,
                    comment = comment.takeIf { it.isNotBlank() },
                    user_id = profile.id,
                    user_email = profile.email,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    suspend fun fetchOpenReports(): List<Report> = firebaseService.fetchReports("open")

    fun resolveReport(report: Report) {
        coroutineScope.launch { firebaseService.resolveReport(report) }
    }

    fun updatePlace(updated: Place) {
        val idx = places.indexOfFirst { it.id == updated.id }
        if (idx >= 0) places[idx] = updated
        coroutineScope.launch { firebaseService.updatePlace(updated) }
    }

    fun assignMaintainer(place: Place, maintainerUserId: String?) {
        updatePlace(place.copy(maintainer_user_id = maintainerUserId))
    }
}

@Composable
fun rememberAppState(firebaseService: FirebaseService): AppState {
    val scope = rememberCoroutineScope()
    return remember(firebaseService) { AppState(firebaseService, scope) }
}

val LocalAppState = compositionLocalOf<AppState> { error("AppState not provided") }
