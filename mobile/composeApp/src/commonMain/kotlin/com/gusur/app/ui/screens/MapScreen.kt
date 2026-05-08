package com.gusur.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.Place
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.components.DateStepButton
import com.gusur.app.ui.components.MapMarker
import com.gusur.app.ui.components.OsmMapView
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
fun MapScreen() {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current

    var maxDistanceKm by remember { mutableStateOf(30.0) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var startDate by remember { mutableStateOf(today) }
    var endDate by remember { mutableStateOf(today.plus(7, DateTimeUnit.DAY)) }

    val placesWithEventsInRange = remember(app.places, app.events, startDate, endDate) {
        val placeIdsWithMatchingEvents = app.events.mapNotNull { event ->
            val date = try { Instant.parse(event.datetime).toLocalDateTime(TimeZone.currentSystemDefault()).date } catch (e: Exception) { null }
            if (date != null && date >= startDate && date <= endDate) event.place_id else null
        }.toSet()
        app.places.filter { it.id in placeIdsWithMatchingEvents }
    }

    val filteredPlaces = remember(placesWithEventsInRange, maxDistanceKm) {
        placesWithEventsInRange.filter { calculateDistance(app.userLat, app.userLon, it.latitude, it.longitude) <= maxDistanceKm }
    }

    var selectedInMap by remember { mutableStateOf<Place?>(null) }

    val markers = remember(filteredPlaces, selectedInMap) {
        filteredPlaces.map { p ->
            MapMarker(
                id = p.id,
                lat = p.latitude,
                lon = p.longitude,
                highlighted = selectedInMap?.id == p.id,
                emoji = if (selectedInMap?.id == p.id) "🔥" else "🧖"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OsmMapView(
            centerLat = app.userLat,
            centerLon = app.userLon,
            markers = markers,
            onMarkerClick = { marker -> selectedInMap = filteredPlaces.firstOrNull { it.id == marker.id } },
            modifier = Modifier.fillMaxSize(),
            initialZoom = 11
        )

        var filtersExpanded by remember { mutableStateOf(false) }
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            border = BorderStroke(2.dp, GusurColors.Ink),
            shape = RoundedCornerShape(20.dp),
            color = GusurColors.Paper.copy(alpha = 0.96f),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { filtersExpanded = !filtersExpanded }) {
                    Icon(Icons.Default.Search, null, tint = GusurColors.Ink, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${filteredPlaces.size} places · ${maxDistanceKm.toInt()}km · ${startDate.dayOfMonth}.${startDate.monthNumber} → ${endDate.dayOfMonth}.${endDate.monthNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(if (filtersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = GusurColors.Ink, modifier = Modifier.size(20.dp))
                }
                if (filtersExpanded) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LocationOn, null, tint = GusurColors.Ink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${maxDistanceKm.toInt()} km", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                        Slider(value = maxDistanceKm.toFloat(), onValueChange = { maxDistanceKm = it.toDouble() }, valueRange = 1f..100f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = GusurColors.Ink, activeTrackColor = GusurColors.Ink))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Icon(Icons.Default.DateRange, null, tint = GusurColors.Ink, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        DateStepButton("From", startDate, today, today.plus(60, DateTimeUnit.DAY)) { startDate = it; if (it > endDate) endDate = it }
                        Spacer(Modifier.width(4.dp))
                        Text("→", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        DateStepButton("To", endDate, startDate, today.plus(60, DateTimeUnit.DAY)) { endDate = it }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { maxDistanceKm = 30.0; startDate = today; endDate = today.plus(7, DateTimeUnit.DAY) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Refresh, "Reset", tint = GusurColors.Ember, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (selectedInMap != null) {
            val placeEvents = app.events.filter { it.place_id == selectedInMap!!.id }
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp, start = 16.dp, end = 16.dp)) {
                Surface(modifier = Modifier.fillMaxWidth(), border = BorderStroke(2.dp, GusurColors.Ink), shape = RoundedCornerShape(24.dp), color = GusurColors.Paper, elevation = 12.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = GusurColors.Ink, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("🧖", fontSize = 24.sp, lineHeight = 24.sp) } }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(selectedInMap!!.name, style = MaterialTheme.typography.h6, maxLines = 1)
                                Text("${placeEvents.size} shows · ★ ${selectedInMap!!.average_rating ?: "N/A"}", style = MaterialTheme.typography.caption)
                            }
                            IconButton(onClick = { selectedInMap = null }) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navigator.push(Screen.PlaceDetail(selectedInMap!!.id)) }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ink, contentColor = Color.White)) {
                            Text("VIEW PLACE →", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
