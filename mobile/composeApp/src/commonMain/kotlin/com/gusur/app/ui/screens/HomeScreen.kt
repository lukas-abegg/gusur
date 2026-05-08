package com.gusur.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.components.DateStepButton
import com.gusur.app.ui.components.EventCard
import com.gusur.app.ui.components.FilterChip
import com.gusur.app.ui.components.calculateDistance
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen() {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val userProfile = app.firebaseService.currentUserProfile.collectAsState().value ?: return

    var filterType by remember { mutableStateOf("All") }
    var maxDistanceKm by remember { mutableStateOf(20.0) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plus(14, DateTimeUnit.DAY)) }

    LaunchedEffect(filterType) {
        if (filterType == "Tonight") { startDate = today; endDate = today }
        else if (filterType == "All") { startDate = today; endDate = today.plus(30, DateTimeUnit.DAY) }
    }

    val filteredEvents = remember(app.events, app.placesById, filterType, maxDistanceKm, startDate, endDate) {
        app.events.filter { event ->
            val place = app.placesById[event.place_id] ?: return@filter false
            val eventDate = try { Instant.parse(event.datetime).toLocalDateTime(TimeZone.currentSystemDefault()).date } catch (e: Exception) { null }
            val dateMatches = eventDate?.let { it >= startDate && it <= endDate } ?: false
            val distance = calculateDistance(app.userLat, app.userLon, place.latitude, place.longitude)
            val distanceMatches = distance <= maxDistanceKm
            val typeMatches = when (filterType) {
                "Top Rated" -> (place.average_rating ?: 0.0) >= 4.5
                else -> true
            }
            dateMatches && distanceMatches && typeMatches
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Hey, ${userProfile.name}", style = MaterialTheme.typography.h5)
                Text("${filteredEvents.size} shows found for you", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink)) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = GusurColors.Ink) } }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                FilterChip("All", filterType == "All") { filterType = "All" }
                Spacer(Modifier.width(8.dp))
                FilterChip("Tonight", filterType == "Tonight") { filterType = "Tonight" }
                Spacer(Modifier.width(8.dp))
                FilterChip("Top Rated", filterType == "Top Rated", isHot = true) { filterType = "Top Rated" }
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = { filterType = "All"; maxDistanceKm = 20.0; startDate = today; endDate = today.plus(14, DateTimeUnit.DAY) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Refresh, "Reset", tint = GusurColors.Ember, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.LocationOn, null, tint = GusurColors.Ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${maxDistanceKm.toInt()} km", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                Slider(value = maxDistanceKm.toFloat(), onValueChange = { maxDistanceKm = it.toDouble() }, valueRange = 1f..100f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = GusurColors.Ink, activeTrackColor = GusurColors.Ink))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, null, tint = GusurColors.Ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                DateStepButton("From", startDate, today, today.plus(30, DateTimeUnit.DAY)) { startDate = it; if (it > endDate) endDate = it }
                Spacer(Modifier.width(4.dp))
                Text("→", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                DateStepButton("To", endDate, startDate, today.plus(30, DateTimeUnit.DAY)) { endDate = it }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (filteredEvents.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No shows found.", color = Color.Gray) } }
            } else {
                items(filteredEvents) { event ->
                    val place = app.placesById[event.place_id]
                    EventCard(event, place, app.userLat, app.userLon) { navigator.push(Screen.EventDetail(event.id)) }
                }
            }
        }
    }
}
