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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
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
import com.gusur.app.model.ReportReasons
import com.gusur.app.model.TargetType
import com.gusur.app.model.canEditPlace
import com.gusur.app.model.canSuggestEdit
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.components.DetailStatCard
import com.gusur.app.ui.components.ReviewCard
import com.gusur.app.ui.components.calculateDistance
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@Composable
fun PlaceDetailScreen(placeId: String) {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val place = app.placeById(placeId) ?: return
    val profile = app.firebaseService.currentUserProfile.collectAsState().value
    val isAdmin = app.firebaseService.isAdmin.collectAsState().value

    val ratings = remember { mutableStateListOf<Rating>() }
    var followerCount by remember { mutableStateOf(0) }

    LaunchedEffect(placeId) {
        ratings.clear()
        ratings.addAll(app.fetchRatings(placeId))
        followerCount = app.fetchPlaceFollowerCount(placeId)
    }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val placeEvents = app.events.filter { it.place_id == placeId }.sortedBy { it.datetime }
    val futureEvents = placeEvents.filter {
        try { Instant.parse(it.datetime).toLocalDateTime(TimeZone.currentSystemDefault()).date >= today } catch (e: Exception) { false }
    }
    val distance = calculateDistance(app.userLat, app.userLon, place.latitude, place.longitude)
    val avg = if (ratings.isNotEmpty()) (ratings.map { it.heat_management }.average() * 10).roundToInt() / 10.0 else place.average_rating ?: 0.0
    val isFollowing = app.isFollowing(placeId)
    val canEdit = profile?.let { canEditPlace(it, place, isAdmin) } ?: false
    val canSuggest = !canEdit && canSuggestEdit(app.myStats)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp).background(Brush.verticalGradient(listOf(GusurColors.WoodDeep, GusurColors.Birch, GusurColors.Gold)))) {
            IconButton(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 40.dp, start = 16.dp)) {
                Surface(shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), modifier = Modifier.size(38.dp), color = GusurColors.Paper) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = GusurColors.Ink, modifier = Modifier.size(24.dp)) }
                }
            }
            Row(modifier = Modifier.padding(top = 40.dp, end = 16.dp).align(Alignment.TopEnd)) {
                if (canEdit) {
                    IconButton(onClick = { navigator.push(Screen.EditPlace(place.id)) }) {
                        Surface(shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), modifier = Modifier.size(38.dp), color = GusurColors.Paper) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Build, "Edit", tint = GusurColors.Ink, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                IconButton(onClick = { navigator.push(Screen.ReportTarget(TargetType.PLACE, place.id)) }) {
                    Surface(shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), modifier = Modifier.size(38.dp), color = GusurColors.Paper) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Warning, "Report", tint = GusurColors.Ember, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Surface(color = GusurColors.Ink, shape = CircleShape) { Text("🧖 PLACE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.overline, color = GusurColors.Gold) }
                Spacer(Modifier.height(8.dp)); Text(place.name, style = MaterialTheme.typography.h4, color = Color.White)
                Text("${distance.toInt()} km away", style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStatCard("$avg", "Rating", isEmber = true, Modifier.weight(1f))
                DetailStatCard("${ratings.size}", "Reviews", false, Modifier.weight(1f))
                DetailStatCard("$followerCount", "Followers", false, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        app.toggleFollow(placeId)
                        followerCount = if (isFollowing) (followerCount - 1).coerceAtLeast(0) else followerCount + 1
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isFollowing) GusurColors.Gold else GusurColors.Ink,
                        contentColor = if (isFollowing) GusurColors.Ink else Color.White
                    )
                ) {
                    Icon(if (isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFollowing) "FOLLOWING" else "FOLLOW", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { navigator.push(Screen.SubmitReview(place.id)) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, GusurColors.Ink)
                ) { Text("REVIEW", fontWeight = FontWeight.ExtraBold, color = GusurColors.Ink, fontSize = 12.sp) }
            }

            if (!place.description.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                Text(place.description, style = MaterialTheme.typography.body2, color = Color.DarkGray)
            }
            if (place.hygiene_info.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(border = BorderStroke(1.dp, GusurColors.Ink), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = GusurColors.Ink)
                        Spacer(Modifier.width(8.dp))
                        Text(place.hygiene_info, fontSize = 12.sp)
                    }
                }
            }
            if (place.amenities.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Amenities: ${place.amenities.joinToString(" · ")}", fontSize = 11.sp, color = Color.Gray)
            }
            place.maintainer_user_id?.let {
                Spacer(Modifier.height(8.dp))
                Text("Maintained by community member", fontSize = 11.sp, color = GusurColors.Ember, fontStyle = FontStyle.Italic)
            }

            if (canSuggest) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { navigator.push(Screen.ReportTarget(TargetType.PLACE, place.id, ReportReasons.SUGGEST_EDIT)) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, GusurColors.Ember)
                ) { Text("SUGGEST AN EDIT", color = GusurColors.Ember, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(32.dp))
            Text("Upcoming shows", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(12.dp))
            if (futureEvents.isEmpty()) {
                Text("No scheduled shows.", color = Color.Gray, fontStyle = FontStyle.Italic)
            } else {
                futureEvents.take(8).forEach { ev ->
                    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { navigator.push(Screen.EventDetail(ev.id)) }) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ev.name, style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
                                Text(
                                    try { Instant.parse(ev.datetime).toLocalDateTime(TimeZone.currentSystemDefault()).let { "${it.date} · ${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}" } } catch (e: Exception) { "TBA" },
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            if (app.isAttending(ev.id)) {
                                Surface(color = GusurColors.Gold, shape = CircleShape) {
                                    Text("Going", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GusurColors.Ink)
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Community Reviews", style = MaterialTheme.typography.h6)
            Spacer(Modifier.height(16.dp))
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
