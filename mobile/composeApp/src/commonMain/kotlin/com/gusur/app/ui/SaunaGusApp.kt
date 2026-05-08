package com.gusur.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gusur.app.api.FirebaseService
import com.gusur.app.theme.GusurColors
import com.gusur.app.theme.GusurTheme
import com.gusur.app.ui.components.GusurBottomNav
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen
import com.gusur.app.ui.navigation.rememberNavigator
import com.gusur.app.ui.navigation.tabScreens
import com.gusur.app.ui.screens.AdminScreen
import com.gusur.app.ui.screens.EditPlaceScreen
import com.gusur.app.ui.screens.EventDetailScreen
import com.gusur.app.ui.screens.HomeScreen
import com.gusur.app.ui.screens.LoginScreen
import com.gusur.app.ui.screens.MapScreen
import com.gusur.app.ui.screens.PlaceDetailScreen
import com.gusur.app.ui.screens.ProfileScreen
import com.gusur.app.ui.screens.ReportScreen
import com.gusur.app.ui.screens.SubmitReviewScreen

@Composable
fun SaunaGusApp() {
    val firebaseService = remember { FirebaseService() }
    val appState = rememberAppState(firebaseService)
    val navigator = rememberNavigator(initial = Screen.Home)

    val userProfile by firebaseService.currentUserProfile.collectAsState()
    val isAdmin by firebaseService.isAdmin.collectAsState()

    LaunchedEffect(userProfile) {
        if (userProfile != null) appState.loadInitialData()
    }

    GusurTheme {
        if (userProfile == null) {
            LoginScreen(
                errorMessage = appState.loginErrorMessage,
                onLogin = { email, pw -> appState.login(email, pw) },
                onRegister = { email, pw, name -> appState.register(email, pw, name) }
            )
            return@GusurTheme
        }

        CompositionLocalProvider(LocalAppState provides appState, LocalNavigator provides navigator) {
            val current = navigator.current
            val isTabRoot = current in tabScreens
            val tabIndex = tabScreens.indexOf(current).coerceAtLeast(0)

            Scaffold(
                bottomBar = {
                    if (isTabRoot) {
                        GusurBottomNav(
                            selectedTabIndex = tabIndex,
                            onTabSelect = { idx ->
                                val target = tabScreens.getOrNull(idx) ?: return@GusurBottomNav
                                if (target == Screen.Admin && !isAdmin) return@GusurBottomNav
                                navigator.selectTab(target)
                            },
                            isAdmin = isAdmin
                        )
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize().background(GusurColors.Paper)) {
                    when (val screen = current) {
                        Screen.Home -> HomeScreen()
                        Screen.Map -> MapScreen()
                        Screen.Profile -> ProfileScreen()
                        Screen.Admin -> AdminScreen()
                        is Screen.EventDetail -> EventDetailScreen(screen.eventId)
                        is Screen.PlaceDetail -> PlaceDetailScreen(screen.placeId)
                        is Screen.SubmitReview -> SubmitReviewScreen(screen.placeId)
                        is Screen.EditPlace -> EditPlaceScreen(screen.placeId)
                        is Screen.ReportTarget -> ReportScreen(screen.targetType, screen.targetId, screen.initialReason)
                    }
                }
            }
        }
    }
}
