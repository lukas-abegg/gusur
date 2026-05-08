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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.computeLevel
import com.gusur.app.model.levelTitle
import com.gusur.app.model.nextLevelHint
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.components.DetailStatCard
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen() {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val profile = app.firebaseService.currentUserProfile.collectAsState().value ?: return

    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember(profile) { mutableStateOf(profile.name) }
    var editedLocation by remember(profile) { mutableStateOf(profile.location) }

    val stats = app.myStats
    val level = computeLevel(stats)
    val progressHint = nextLevelHint(stats)
    val followedPlaces = app.places.filter { it.id in app.followedPlaceIds }
    val attendingEvents = app.events.filter { it.id in app.attendingEventIds }
    val attendedEvents = app.events.filter { it.id in app.attendedEventIds }

    Column(modifier = Modifier.fillMaxSize().background(GusurColors.Paper).verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Brush.linearGradient(listOf(GusurColors.Birch, GusurColors.WoodDeep)))) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).offset(y = 48.dp).size(96.dp), shape = CircleShape, color = GusurColors.Gold, border = BorderStroke(4.dp, GusurColors.Paper), elevation = 6.dp) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(profile.name.take(1).uppercase(), style = MaterialTheme.typography.h3, lineHeight = 40.sp) }
            }
        }
        Column(modifier = Modifier.padding(top = 56.dp).fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (isEditing) {
                OutlinedTextField(value = editedName, onValueChange = { editedName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = editedLocation, onValueChange = { editedLocation = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Button(onClick = { app.saveUserProfile(profile.copy(name = editedName, location = editedLocation)); isEditing = false }, modifier = Modifier.fillMaxWidth()) { Text("SAVE CHANGES") }
            } else {
                Text(profile.name, style = MaterialTheme.typography.h5)
                Text("${profile.email} · ${profile.location}", style = MaterialTheme.typography.caption, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { isEditing = true }, colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ink, contentColor = Color.White), shape = CircleShape) { Text("EDIT PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            }
            Row(modifier = Modifier.padding(top = 24.dp)) {
                Surface(color = GusurColors.Ink, shape = CircleShape) { Text("🔥 ${levelTitle(level).uppercase()}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.overline, color = GusurColors.Gold) }
                Spacer(Modifier.width(8.dp))
                Surface(border = BorderStroke(2.dp, GusurColors.Ink), shape = CircleShape, color = GusurColors.Paper) { Text("LEVEL $level", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.overline) }
            }
            if (progressHint != null) {
                Spacer(Modifier.height(8.dp))
                Text(progressHint, fontSize = 11.sp, color = GusurColors.Ember, fontStyle = FontStyle.Italic)
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Your contributions", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStatCard("${stats.reviewsWritten}", "Reviews", isEmber = true, Modifier.weight(1f))
                DetailStatCard("${stats.eventsAttended}", "Attended", false, Modifier.weight(1f))
                DetailStatCard("${followedPlaces.size}", "Following", false, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("RSVPs are tracked but only check-ins count toward level. ${stats.eventsRSVPd} active RSVPs.", fontSize = 10.sp, color = Color.Gray, fontStyle = FontStyle.Italic)

            Spacer(Modifier.height(24.dp))
            Text("Places you follow", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))
            if (followedPlaces.isEmpty()) {
                Text("Follow a place to see it here.", color = Color.Gray, fontStyle = FontStyle.Italic)
            } else {
                followedPlaces.take(8).forEach { p ->
                    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { navigator.push(Screen.PlaceDetail(p.id)) }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                Text("★ ${p.average_rating ?: "N/A"} · ${p.address}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Shows you're going to", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))
            if (attendingEvents.isEmpty()) {
                Text("RSVP to a show to see it here.", color = Color.Gray, fontStyle = FontStyle.Italic)
            } else {
                attendingEvents.take(8).forEach { ev ->
                    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { navigator.push(Screen.EventDetail(ev.id)) }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.name, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                Text(app.placeById(ev.place_id)?.name ?: "Unknown place", fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(color = GusurColors.Gold, shape = CircleShape) {
                                Text("Going", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GusurColors.Ink)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (attendedEvents.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("Shows you've attended", style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(12.dp))
                attendedEvents.take(8).forEach { ev ->
                    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { navigator.push(Screen.EventDetail(ev.id)) }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.name, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                Text(app.placeById(ev.place_id)?.name ?: "Unknown place", fontSize = 11.sp, color = Color.Gray)
                            }
                            Surface(color = GusurColors.Ember, shape = CircleShape) {
                                Text("Attended", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { app.coroutineScope.launch { app.firebaseService.logout() } },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                border = BorderStroke(1.5.dp, GusurColors.Ember)
            ) { Text("LOG OUT", color = GusurColors.Ember, fontWeight = FontWeight.Bold) }
        }
    }
}
