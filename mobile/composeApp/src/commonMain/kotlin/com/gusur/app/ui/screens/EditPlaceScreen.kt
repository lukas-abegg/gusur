package com.gusur.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.navigation.LocalNavigator

@Composable
fun EditPlaceScreen(placeId: String) {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val place = app.placeById(placeId) ?: return

    var address by remember(place) { mutableStateOf(place.address) }
    var description by remember(place) { mutableStateOf(place.description ?: "") }
    var hygieneInfo by remember(place) { mutableStateOf(place.hygiene_info) }
    var amenities by remember(place) { mutableStateOf(place.amenities.joinToString(", ")) }

    Column(modifier = Modifier.fillMaxSize().background(GusurColors.Paper)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(modifier = Modifier.size(36.dp).clickable { navigator.pop() }, border = BorderStroke(2.dp, GusurColors.Ink), shape = RoundedCornerShape(12.dp), color = GusurColors.Paper) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = GusurColors.Ink, modifier = Modifier.size(22.dp)) }
            }
            Text("Edit place", style = MaterialTheme.typography.h6)
            Spacer(Modifier.width(36.dp))
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text(place.name, style = MaterialTheme.typography.h5)
            Text("Maintainer-only fields. Lat/long and id are immutable here.", fontSize = 11.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), maxLines = 8)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = hygieneInfo, onValueChange = { hygieneInfo = it }, label = { Text("Hygiene info") }, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), maxLines = 4)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = amenities, onValueChange = { amenities = it }, label = { Text("Amenities (comma-separated)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    app.updatePlace(
                        place.copy(
                            address = address,
                            description = description.takeIf { it.isNotBlank() },
                            hygiene_info = hygieneInfo,
                            amenities = amenities.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        )
                    )
                    navigator.pop()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ink, contentColor = Color.White)
            ) { Text("SAVE PLACE", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}
