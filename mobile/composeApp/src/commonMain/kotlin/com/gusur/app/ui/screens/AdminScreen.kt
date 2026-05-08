package com.gusur.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.model.Report
import com.gusur.app.model.TargetType
import com.gusur.app.theme.GusurColors
import com.gusur.app.ui.LocalAppState
import com.gusur.app.ui.navigation.LocalNavigator
import com.gusur.app.ui.navigation.Screen

@Composable
fun AdminScreen() {
    val app = LocalAppState.current
    val navigator = LocalNavigator.current

    val openReports = remember { mutableStateListOf<Report>() }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        openReports.clear()
        openReports.addAll(app.fetchOpenReports())
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Admin Dashboard", style = MaterialTheme.typography.h4)
        Text("${app.places.size} places · ${app.events.size} events · ${openReports.size} open reports", style = MaterialTheme.typography.caption, color = Color.Gray)

        Button(
            onClick = { app.loadFromScraper() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Gold, contentColor = GusurColors.Ink)
        ) {
            Text("LOAD FROM ../scraper/ (Desktop only)")
        }
        Button(onClick = { app.syncToFirestore() }, modifier = Modifier.fillMaxWidth()) {
            Text("SYNC PLACES & EVENTS TO FIRESTORE")
        }
        Button(
            onClick = { app.migrateLegacyInfusions() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ember, contentColor = Color.White)
        ) {
            Text("MIGRATE LEGACY INFUSIONS → PLACES + EVENTS")
        }

        Spacer(Modifier.height(8.dp))
        Text("Open reports", style = MaterialTheme.typography.h6)
        if (openReports.isEmpty()) {
            Text("Nothing to review. 🎉", color = Color.Gray, fontStyle = FontStyle.Italic)
        } else {
            openReports.forEach { report ->
                val targetName = when (report.target_type) {
                    TargetType.PLACE -> app.placeById(report.target_id)?.name ?: report.target_id
                    TargetType.EVENT -> app.eventById(report.target_id)?.name ?: report.target_id
                    else -> report.target_id
                }
                Surface(border = BorderStroke(1.5.dp, GusurColors.Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = GusurColors.Ember, shape = RoundedCornerShape(6.dp)) {
                                Text(report.reason.replace('_', ' ').uppercase(), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(report.target_type.uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(targetName, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            when (report.target_type) {
                                TargetType.PLACE -> navigator.push(Screen.PlaceDetail(report.target_id))
                                TargetType.EVENT -> navigator.push(Screen.EventDetail(report.target_id))
                            }
                        })
                        if (!report.comment.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(report.comment, fontSize = 12.sp, color = Color.DarkGray)
                        }
                        Text("by ${report.user_email.substringBefore('@')}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    app.resolveReport(report)
                                    refreshKey++
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("RESOLVE", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            if (report.target_type == TargetType.PLACE) {
                                Button(
                                    onClick = { navigator.push(Screen.EditPlace(report.target_id)) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Ink, contentColor = Color.White)
                                ) { Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Reports queue lives in the `reports` Firestore collection. Resolving sets status='resolved'. Place edits go through the maintainer flow — assign a maintainer by editing a Place's maintainer_user_id field.",
            fontSize = 10.sp,
            color = Color.Gray,
            fontStyle = FontStyle.Italic
        )
    }
}
