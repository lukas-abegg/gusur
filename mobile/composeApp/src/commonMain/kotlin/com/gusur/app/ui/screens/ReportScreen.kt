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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.ReportReasons
import com.gusur.app.model.TargetType
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.navigation.LocalNavigator

@Composable
fun ReportScreen(targetType: String, targetId: String, initialReason: String? = null) {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current

    val targetName = when (targetType) {
        TargetType.PLACE -> app.placeById(targetId)?.name ?: "this place"
        TargetType.EVENT -> app.eventById(targetId)?.name ?: "this show"
        else -> "this listing"
    }

    val isSuggestEdit = initialReason == ReportReasons.SUGGEST_EDIT
    var selectedReason by remember { mutableStateOf(initialReason ?: ReportReasons.WRONG_INFO) }
    var comment by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(GusurColors.Cream)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(modifier = Modifier.size(36.dp).clickable { navigator.pop() }, border = BorderStroke(2.dp, GusurColors.Ink), shape = RoundedCornerShape(12.dp), color = GusurColors.Paper) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Back", tint = GusurColors.Ink, modifier = Modifier.size(22.dp)) }
            }
            Text(if (isSuggestEdit) "Suggest edit" else "Report", style = MaterialTheme.typography.h6)
            Spacer(Modifier.width(36.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
            Text(if (isSuggestEdit) "Suggest an edit for $targetName" else "What's wrong with $targetName?", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            if (!isSuggestEdit) {
                ReportReasons.all.filter { it != ReportReasons.SUGGEST_EDIT }.forEach { reason ->
                    val active = reason == selectedReason
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedReason = reason },
                        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) GusurColors.Ember else GusurColors.Ink),
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) GusurColors.Ember.copy(alpha = 0.1f) else GusurColors.Paper
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(18.dp), shape = CircleShape, border = BorderStroke(2.dp, if (active) GusurColors.Ember else GusurColors.Ink), color = if (active) GusurColors.Ember else Color.Transparent) {}
                            Spacer(Modifier.width(12.dp))
                            Text(reasonLabel(reason), fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Text(if (isSuggestEdit) "What should change?" else "Details (optional)", style = MaterialTheme.typography.subtitle2, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text(if (isSuggestEdit) "Describe the edit — wrong opening hours, missing amenity, better photo, etc." else "What did you notice? Any specifics that help moderators.", fontSize = 13.sp, color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = GusurColors.Ink, unfocusedBorderColor = GusurColors.Ink.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    app.submitReport(targetType, targetId, selectedReason, comment)
                    navigator.pop()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ember, contentColor = Color.White)
            ) { Text(if (isSuggestEdit) "SUBMIT SUGGESTION" else "SUBMIT REPORT", fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun reasonLabel(reason: String): String = when (reason) {
    ReportReasons.CLOSED -> "Place is closed / no longer running"
    ReportReasons.WRONG_INFO -> "Wrong information (hours, address, etc.)"
    ReportReasons.DUPLICATE -> "Duplicate listing"
    ReportReasons.INAPPROPRIATE -> "Inappropriate content"
    ReportReasons.SUGGEST_EDIT -> "Suggest an edit"
    ReportReasons.OTHER -> "Other"
    else -> reason
}
