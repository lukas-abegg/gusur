package com.gusur.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.Event
import com.gusur.app.model.Place
import com.gusur.app.theme.GusurColors
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun EventCard(event: Event, place: Place?, userLat: Double, userLon: Double, onClick: () -> Unit) {
    val distance = place?.let { calculateDistance(userLat, userLon, it.latitude, it.longitude) }
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, border = BorderStroke(2.dp, GusurColors.Ink), shape = RoundedCornerShape(24.dp), color = Color.White) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.name, style = MaterialTheme.typography.h6)
                    Text(
                        buildString {
                            append(place?.name ?: "Unknown place")
                            distance?.let { append(" · ${it.toInt()}km away") }
                        },
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }
                Surface(color = GusurColors.Gold, shape = CircleShape) {
                    Text("★ ${place?.average_rating ?: "N/A"}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.overline, color = GusurColors.Ink, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconText(Icons.Default.DateRange, try { Instant.parse(event.datetime).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() } catch (e: Exception) { "TBA" })
                IconText(Icons.Default.Favorite, "${event.heat_level}° Heat")
                IconText(Icons.Default.Notifications, "${event.length_minutes}m")
            }
        }
    }
}

@Composable
fun ReviewCard(name: String, rate: String, body: String) {
    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, border = BorderStroke(1.5.dp, GusurColors.Ink), color = GusurColors.Birch) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.ExtraBold) } }
                Spacer(Modifier.width(8.dp)); Text(name.substringBefore("@"), fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("Heat: $rate/7", color = GusurColors.Ember, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp)); Text(body, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun FilterChip(label: String, active: Boolean, isHot: Boolean = false, onClick: () -> Unit) {
    Surface(border = BorderStroke(1.5.dp, if (isHot) GusurColors.Ember else GusurColors.Ink), shape = CircleShape, color = if (active) GusurColors.Ink else Color.Transparent, contentColor = if (active) GusurColors.Paper else if (isHot) GusurColors.Ember else GusurColors.Ink, modifier = Modifier.clickable { onClick() }) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun IconText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun DetailStatCard(num: String, label: String, isEmber: Boolean = false, modifier: Modifier = Modifier) {
    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(num, style = MaterialTheme.typography.h5, color = if (isEmber) GusurColors.Ember else GusurColors.Ink)
            Text(label, style = MaterialTheme.typography.overline, color = Color.Gray)
        }
    }
}

@Composable
fun DetailMetaRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.body2, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DateStepButton(label: String, date: LocalDate, minDate: LocalDate, maxDate: LocalDate, onDateChange: (LocalDate) -> Unit) {
    Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(8.dp), color = GusurColors.Paper) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("$label: ${date.dayOfMonth}.${date.monthNumber}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Column {
                Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(12.dp).clickable { if (date < maxDate) onDateChange(date.plus(1, DateTimeUnit.DAY)) })
                Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(12.dp).clickable { if (date > minDate) onDateChange(date.minus(1, DateTimeUnit.DAY)) })
            }
        }
    }
}

@Composable
fun GusurBottomNav(selectedTabIndex: Int, onTabSelect: (Int) -> Unit, isAdmin: Boolean) {
    Surface(color = GusurColors.Ink, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            NavTabItem(Icons.Default.Home, "Home", selectedTabIndex == 0) { onTabSelect(0) }
            NavTabItem(Icons.Default.Search, "Discover", selectedTabIndex == 1) { onTabSelect(1) }
            NavTabItem(Icons.Default.Person, "Profile", selectedTabIndex == 2) { onTabSelect(2) }
            if (isAdmin) NavTabItem(Icons.Default.Settings, "Admin", selectedTabIndex == 3) { onTabSelect(3) }
        }
    }
}

@Composable
fun NavTabItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }.alpha(if (isSelected) 1f else 0.5f)) {
        Icon(icon, null, tint = if (isSelected) GusurColors.Gold else Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = if (isSelected) GusurColors.Gold else Color.White, fontWeight = FontWeight.Bold)
    }
}

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0
    val a = sin(dLat / 2).pow(2) + cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}
