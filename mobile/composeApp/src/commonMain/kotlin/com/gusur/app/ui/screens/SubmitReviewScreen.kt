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
fun SubmitReviewScreen(placeId: String) {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current
    val place = app.placeById(placeId) ?: return
    val userProfile = app.firebaseService.currentUserProfile.collectAsState().value ?: return

    var selectedHeat by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(GusurColors.Cream)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(modifier = Modifier.size(36.dp).clickable { navigator.pop() }, border = BorderStroke(2.dp, GusurColors.Ink), shape = RoundedCornerShape(12.dp), color = GusurColors.Paper) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = GusurColors.Ink, modifier = Modifier.size(22.dp)) } }
            Text("Rate ${place.name}", style = MaterialTheme.typography.h6)
            Text("Skip", modifier = Modifier.clickable { navigator.pop() }, color = GusurColors.Ember, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text("How hot was it?", style = MaterialTheme.typography.h4, fontStyle = FontStyle.Italic)
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { i ->
                    val num = i + 1; val isSelected = num == selectedHeat
                    Surface(modifier = Modifier.size(if (isSelected) 42.dp else 38.dp).clickable { selectedHeat = num }, shape = CircleShape, border = BorderStroke(2.dp, GusurColors.Ink), color = if (num <= selectedHeat) GusurColors.Ember else GusurColors.Paper, elevation = if (isSelected) 4.dp else 0.dp) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("$num", color = if (num <= selectedHeat) Color.White else GusurColors.Ink, fontWeight = FontWeight.ExtraBold, lineHeight = 14.sp) }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text("Share your thoughts", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("What made it special? Heat, scents, atmosphere...", fontSize = 13.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = GusurColors.Ink, unfocusedBorderColor = GusurColors.Ink.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                maxLines = 8
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    app.submitReview(place.id, userProfile.id, userProfile.email, selectedHeat, comment)
                    navigator.pop()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ink, contentColor = Color.White)
            ) { Text("SUBMIT REVIEW", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}
