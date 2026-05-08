package com.gusur.app.ui.navigation

sealed class Screen {
    data object Home : Screen()
    data object Map : Screen()
    data object Profile : Screen()
    data object Admin : Screen()
    data class EventDetail(val eventId: String) : Screen()
    data class PlaceDetail(val placeId: String) : Screen()
    data class SubmitReview(val placeId: String) : Screen()
    data class EditPlace(val placeId: String) : Screen()
    data class ReportTarget(val targetType: String, val targetId: String, val initialReason: String? = null) : Screen()
}

val tabScreens = listOf(Screen.Home, Screen.Map, Screen.Profile, Screen.Admin)
