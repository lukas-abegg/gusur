package com.gusur.app.api

import com.gusur.app.model.Attendance
import com.gusur.app.model.AttendanceStatus
import com.gusur.app.model.Event
import com.gusur.app.model.Follow
import com.gusur.app.model.LegacyInfusion
import com.gusur.app.model.Place
import com.gusur.app.model.Rating
import com.gusur.app.model.Report
import com.gusur.app.model.UserProfile
import com.gusur.app.model.UserStats
import com.gusur.app.model.attendanceId
import com.gusur.app.model.deriveSchema
import com.gusur.app.model.followId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Clock

class FirebaseService {
    val auth by lazy { try { Firebase.auth } catch (e: Exception) { null } }
    val db by lazy { try { Firebase.firestore } catch (e: Exception) { null } }

    val currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val isAdmin = MutableStateFlow(false)
    val ratings = MutableStateFlow<List<Rating>>(emptyList())
    val attendances = MutableStateFlow<List<Attendance>>(emptyList())
    val follows = MutableStateFlow<List<Follow>>(emptyList())
    val reports = MutableStateFlow<List<Report>>(emptyList())

    suspend fun login(email: String, password: String): Pair<Boolean, String?> {
        if (auth == null) {
            println("Firebase not connected, using fallback login logic.")
            if (email == "admin@gusur.com") {
                if (password == "password") {
                    currentUserProfile.value = UserProfile(id = "dummy", email = email, name = "Admin User")
                    isAdmin.value = true
                    return true to null
                } else {
                    return false to "Incorrect password for admin."
                }
            }
            if (email.isNotBlank()) {
                if (password.length >= 6) {
                    currentUserProfile.value = UserProfile(id = "test-user", email = email, name = "Test User")
                    isAdmin.value = false
                    return true to null
                } else {
                    return false to "Password must be at least 6 characters."
                }
            }
            return false to "Email cannot be empty."
        }
        return try {
            val result = auth?.signInWithEmailAndPassword(email, password)
            if (result != null) {
                loadUserProfile(result.user?.uid ?: "")
                true to null
            } else false to "Login failed. Please check your credentials."
        } catch (e: Exception) {
            println("Login failed: ${e.message}")
            val msg = when {
                e.message?.contains("password") == true -> "Incorrect password."
                e.message?.contains("user") == true -> "User not found."
                else -> e.message ?: "An unknown error occurred."
            }
            false to msg
        }
    }

    suspend fun register(email: String, password: String, name: String): Pair<Boolean, String?> {
        return try {
            val result = auth?.createUserWithEmailAndPassword(email, password)
            if (result != null) {
                val uid = result.user?.uid ?: ""
                val profile = UserProfile(id = uid, email = email, name = name)
                saveUserProfile(profile)
                loadUserProfile(uid)
                true to null
            } else false to "Registration failed."
        } catch (e: Exception) {
            println("Registration failed: ${e.message}")
            false to (e.message ?: "Registration failed.")
        }
    }

    suspend fun logout() {
        auth?.signOut()
        currentUserProfile.value = null
        isAdmin.value = false
        ratings.value = emptyList()
    }

    private suspend fun loadUserProfile(uid: String) {
        try {
            val snapshot = db?.collection("profiles")?.document(uid)?.get()
            if (snapshot?.exists == true) {
                val profile = snapshot.data<UserProfile>()
                currentUserProfile.value = profile
                isAdmin.value = profile.email == "lukas.abegg1@gmail.com"
            }
        } catch (e: Exception) {
            println("Load profile failed: ${e.message}")
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        try {
            db?.collection("profiles")?.document(profile.id)?.set(profile)
            currentUserProfile.value = profile
        } catch (e: Exception) {
            println("Save profile failed: ${e.message}")
        }
    }

    // --- Places ---

    suspend fun fetchPlaces(): List<Place> {
        return try {
            val snapshot = db?.collection("places")?.get()
            snapshot?.documents?.map { it.data<Place>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch places failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncPlaces(places: List<Place>) {
        try {
            places.forEach { place ->
                db?.collection("places")?.document(place.id)?.set(place)
            }
            println("Synced ${places.size} places to Firestore")
        } catch (e: Exception) {
            println("Sync places failed: ${e.message}")
        }
    }

    // --- Events ---

    suspend fun fetchEvents(): List<Event> {
        return try {
            val snapshot = db?.collection("events")?.get()
            snapshot?.documents?.map { it.data<Event>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch events failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun syncEvents(events: List<Event>) {
        try {
            events.forEach { event ->
                db?.collection("events")?.document(event.id)?.set(event)
            }
            println("Synced ${events.size} events to Firestore")
        } catch (e: Exception) {
            println("Sync events failed: ${e.message}")
        }
    }

    // --- Legacy migration ---

    suspend fun fetchLegacyInfusions(): List<LegacyInfusion> {
        return try {
            val snapshot = db?.collection("infusions")?.get()
            snapshot?.documents?.map { it.data<LegacyInfusion>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch legacy infusions failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun migrateLegacyInfusions(): Triple<Int, Int, Int> {
        val legacy = fetchLegacyInfusions()
        if (legacy.isEmpty()) return Triple(0, 0, 0)
        val (places, events) = deriveSchema(legacy)
        syncPlaces(places)
        syncEvents(events)
        return Triple(legacy.size, places.size, events.size)
    }

    // --- Ratings ---

    suspend fun fetchRatings(placeId: String): List<Rating> {
        if (db == null) {
            return ratings.value.filter { it.place_id == placeId }
        }
        return try {
            val snapshot = db?.collection("ratings")?.where { "place_id" equalTo placeId }?.get()
            snapshot?.documents?.map { it.data<Rating>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch ratings failed: ${e.message}")
            ratings.value.filter { it.place_id == placeId }
        }
    }

    suspend fun submitRating(rating: Rating) {
        if (db == null) {
            println("Firebase not connected, saving rating locally.")
            ratings.value = ratings.value + rating.copy(id = "local_${Clock.System.now().toEpochMilliseconds()}")
            return
        }
        try {
            db?.collection("ratings")?.add(rating)
        } catch (e: Exception) {
            println("Rating submission failed: ${e.message}")
            ratings.value = ratings.value + rating.copy(id = "local_${Clock.System.now().toEpochMilliseconds()}")
        }
    }

    // --- Place updates (maintainer/admin) ---

    suspend fun updatePlace(place: Place) {
        try {
            db?.collection("places")?.document(place.id)?.set(place)
        } catch (e: Exception) {
            println("Update place failed: ${e.message}")
        }
    }

    // --- Attendance (RSVP + check-in) ---

    suspend fun setAttendance(eventId: String, placeId: String, userId: String, going: Boolean) {
        val id = attendanceId(eventId, userId)
        if (db == null) {
            attendances.value = if (going) {
                attendances.value.filter { it.id != id } + Attendance(id = id, event_id = eventId, place_id = placeId, user_id = userId, status = AttendanceStatus.GOING, timestamp = Clock.System.now().toEpochMilliseconds())
            } else {
                attendances.value.filter { it.id != id }
            }
            return
        }
        try {
            if (going) {
                val a = Attendance(id = id, event_id = eventId, place_id = placeId, user_id = userId, status = AttendanceStatus.GOING, timestamp = Clock.System.now().toEpochMilliseconds())
                db?.collection("attendances")?.document(id)?.set(a)
            } else {
                db?.collection("attendances")?.document(id)?.delete()
            }
        } catch (e: Exception) {
            println("Set attendance failed: ${e.message}")
        }
    }

    suspend fun checkInToEvent(eventId: String, placeId: String, userId: String) {
        val id = attendanceId(eventId, userId)
        val attended = Attendance(
            id = id,
            event_id = eventId,
            place_id = placeId,
            user_id = userId,
            status = AttendanceStatus.ATTENDED,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
        if (db == null) {
            attendances.value = attendances.value.filter { it.id != id } + attended
            return
        }
        try {
            db?.collection("attendances")?.document(id)?.set(attended)
        } catch (e: Exception) {
            println("Check-in failed: ${e.message}")
        }
    }

    suspend fun fetchUserAttendances(userId: String): List<Attendance> {
        if (db == null) return attendances.value.filter { it.user_id == userId }
        return try {
            val snapshot = db?.collection("attendances")?.where { "user_id" equalTo userId }?.get()
            snapshot?.documents?.map { it.data<Attendance>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch user attendances failed: ${e.message}")
            attendances.value.filter { it.user_id == userId }
        }
    }

    suspend fun fetchEventAttendees(eventId: String): List<Attendance> {
        if (db == null) return attendances.value.filter { it.event_id == eventId }
        return try {
            val snapshot = db?.collection("attendances")?.where { "event_id" equalTo eventId }?.get()
            snapshot?.documents?.map { it.data<Attendance>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch event attendees failed: ${e.message}")
            attendances.value.filter { it.event_id == eventId }
        }
    }

    /**
     * Counters derived from authoritative collections. No client-side counter to inflate.
     */
    suspend fun fetchUserStats(userId: String): UserStats {
        val reviewCount = try {
            val snapshot = db?.collection("ratings")?.where { "user_id" equalTo userId }?.get()
            snapshot?.documents?.size ?: ratings.value.count { it.user_id == userId }
        } catch (e: Exception) {
            ratings.value.count { it.user_id == userId }
        }
        val userAttendances = fetchUserAttendances(userId)
        return UserStats(
            reviewsWritten = reviewCount,
            eventsAttended = userAttendances.count { it.status == AttendanceStatus.ATTENDED },
            eventsRSVPd = userAttendances.size
        )
    }

    // --- Follows ---

    suspend fun setFollow(placeId: String, userId: String, following: Boolean) {
        val id = followId(placeId, userId)
        if (db == null) {
            follows.value = if (following) {
                follows.value.filter { it.id != id } + Follow(id = id, place_id = placeId, user_id = userId, timestamp = Clock.System.now().toEpochMilliseconds())
            } else {
                follows.value.filter { it.id != id }
            }
            return
        }
        try {
            if (following) {
                val f = Follow(id = id, place_id = placeId, user_id = userId, timestamp = Clock.System.now().toEpochMilliseconds())
                db?.collection("follows")?.document(id)?.set(f)
            } else {
                db?.collection("follows")?.document(id)?.delete()
            }
        } catch (e: Exception) {
            println("Set follow failed: ${e.message}")
        }
    }

    suspend fun fetchUserFollows(userId: String): List<Follow> {
        if (db == null) return follows.value.filter { it.user_id == userId }
        return try {
            val snapshot = db?.collection("follows")?.where { "user_id" equalTo userId }?.get()
            snapshot?.documents?.map { it.data<Follow>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch user follows failed: ${e.message}")
            follows.value.filter { it.user_id == userId }
        }
    }

    suspend fun fetchPlaceFollowers(placeId: String): List<Follow> {
        if (db == null) return follows.value.filter { it.place_id == placeId }
        return try {
            val snapshot = db?.collection("follows")?.where { "place_id" equalTo placeId }?.get()
            snapshot?.documents?.map { it.data<Follow>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch place followers failed: ${e.message}")
            follows.value.filter { it.place_id == placeId }
        }
    }

    // --- Reports ---

    suspend fun submitReport(report: Report) {
        if (db == null) {
            reports.value = reports.value + report.copy(id = "local_${Clock.System.now().toEpochMilliseconds()}")
            return
        }
        try {
            db?.collection("reports")?.add(report)
        } catch (e: Exception) {
            println("Submit report failed: ${e.message}")
            reports.value = reports.value + report.copy(id = "local_${Clock.System.now().toEpochMilliseconds()}")
        }
    }

    suspend fun fetchReports(status: String? = "open"): List<Report> {
        if (db == null) return reports.value.let { all -> if (status == null) all else all.filter { it.status == status } }
        return try {
            val query = if (status == null) {
                db?.collection("reports")
            } else {
                db?.collection("reports")?.where { "status" equalTo status }
            }
            val snapshot = query?.get()
            snapshot?.documents?.map { it.data<Report>() } ?: emptyList()
        } catch (e: Exception) {
            println("Fetch reports failed: ${e.message}")
            reports.value.let { all -> if (status == null) all else all.filter { it.status == status } }
        }
    }

    suspend fun resolveReport(report: Report) {
        val updated = report.copy(status = "resolved")
        if (db == null) {
            reports.value = reports.value.map { if (it.id == report.id) updated else it }
            return
        }
        try {
            if (report.id.isNotBlank()) {
                db?.collection("reports")?.document(report.id)?.set(updated)
            }
        } catch (e: Exception) {
            println("Resolve report failed: ${e.message}")
        }
    }
}
