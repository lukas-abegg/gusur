package com.gusur.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.Rating
import com.gusur.app.model.TargetType
import com.gusur.app.theme.GusurColors
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.components.DetailMetaRow
import com.gusur.app.ui.components.DetailStatCard
import com.gusur.app.ui.components.ReviewCard
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen
import kotlin.math.roundToInt

@Composable
fun EventDetailScreen(eventId: String) {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val event = app.eventById(eventId) ?: return
    val place = app.placeById(event.place_id)

    val ratings = remember { mutableStateListOf<Rating>() }
    var attendeeCount by remember { mutableStateOf(0) }

    LaunchedEffect(eventId) {
        ratings.clear()
        place?.let { ratings.addAll(app.fetchRatings(it.id)) }
        attendeeCount = app.fetchEventAttendees(eventId)
    }

    val isAttending = app.isAttending(eventId)
    val hasAttended = app.hasAttended(eventId)
    val canCheckIn = remember(event) {
        try {
            val eventInstant = Instant.parse(event.datetime)
            val now = Clock.System.now()
            val deltaMinutes = (eventInstant - now).inWholeMinutes
            // Allow check-in from 6h before to 24h after the scheduled start.
            deltaMinutes in -1440..360
        } catch (e: Exception) { false }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(Brush.verticalGradient(listOf(GusurColors.EmberDeep, GusurColors.Ember, GusurColors.Gold)))) {
            IconButton(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 40.dp, start = 16.dp)) {
                Surface(shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), modifier = Modifier.size(38.dp), color = GusurColors.Paper) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = GusurColors.Ink, modifier = Modifier.size(24.dp)) }
                }
            }
            IconButton(onClick = { navigator.push(Screen.ReportTarget(TargetType.EVENT, event.id)) }, modifier = Modifier.padding(top = 40.dp, end = 16.dp).align(Alignment.TopEnd)) {
                Surface(shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), modifier = Modifier.size(38.dp), color = GusurColors.Paper) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, "Report", tint = GusurColors.Ember, modifier = Modifier.size(18.dp)) }
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Surface(color = GusurColors.Ink, shape = CircleShape) { Text("🔥 SIGNATURE SHOW", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.overline, color = GusurColors.Gold) }
                Spacer(Modifier.height(8.dp)); Text(event.name, style = MaterialTheme.typography.h4, color = Color.White)
            }
        }
        Column(modifier = Modifier.padding(20.dp)) {
            val avg = if (ratings.isNotEmpty()) (ratings.map { it.heat_management }.average() * 10).roundToInt() / 10.0 else place?.average_rating ?: 0.0
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStatCard("$avg", "Place Rating", isEmber = true, Modifier.weight(1f))
                DetailStatCard("${event.heat_level}°", "Heat", false, Modifier.weight(1f))
                DetailStatCard("$attendeeCount", "Going", false, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))

            when {
                hasAttended -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = GusurColors.Ember,
                        border = BorderStroke(2.dp, GusurColors.Ink)
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("YOU ATTENDED", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                canCheckIn && isAttending -> {
                    Button(
                        onClick = { place?.let { app.checkIn(eventId, it.id) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ember, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CHECK IN", fontWeight = FontWeight.ExtraBold)
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            place?.let {
                                app.toggleAttendance(eventId, it.id)
                                attendeeCount = if (isAttending) (attendeeCount - 1).coerceAtLeast(0) else attendeeCount + 1
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isAttending) GusurColors.Gold else GusurColors.Ember,
                            contentColor = if (isAttending) GusurColors.Ink else Color.White
                        )
                    ) {
                        if (isAttending) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("YOU'RE GOING", fontWeight = FontWeight.ExtraBold)
                        } else {
                            Text("I'M GOING", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp)); Text("Show details", style = MaterialTheme.typography.h6); Spacer(Modifier.height(12.dp))
            Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { place?.let { navigator.push(Screen.PlaceDetail(it.id)) } }) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GusurColors.Ember)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(place?.name ?: "Unknown place", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                        Text("View place details, reviews & other shows", style = MaterialTheme.typography.caption, color = Color.Gray)
                    }
                    Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            DetailMetaRow("Sauna Meister", event.sauna_meister)
            DetailMetaRow("Style", event.style)
            DetailMetaRow("Price", "${event.price} ${event.currency}")
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { place?.let { navigator.push(Screen.SubmitReview(it.id)) } },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, GusurColors.Ink)
            ) { Text("REVIEW THIS PLACE", fontWeight = FontWeight.ExtraBold, color = GusurColors.Ink) }
            Spacer(Modifier.height(40.dp)); Text("Reviews for ${place?.name ?: "this place"}", style = MaterialTheme.typography.h6); Spacer(Modifier.height(16.dp))
            if (ratings.isEmpty()) {
                Text("No reviews yet. Be the first to rate this place!", color = Color.Gray, fontStyle = FontStyle.Italic)
            } else {
                ratings.sortedByDescending { it.timestamp }.forEach { rating ->
                    ReviewCard(rating.user_email, "${rating.heat_management}", rating.comment ?: "Rated ${rating.heat_management}/7")
                }
            }
        }
    }
}
