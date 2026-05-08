package com.gusur.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gusur.app.model.Event
import com.gusur.app.model.Place
import com.gusur.app.api.SaunaAIService
import kotlinx.coroutines.launch

@Composable
fun AIScreen(events: List<Event>, placesById: Map<String, Place>) {
    // In a real app, this API key should be managed securely
    val aiService = remember { SaunaAIService("YOUR_FREE_GEMINI_KEY") }
    val response by aiService.aiResponse.collectAsState()
    val isLoading by aiService.isLoading.collectAsState()
    var question by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF003366), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Face, null, tint = Color.White, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Gusur AI Assistant", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = Color(0xFF003366))
                Text("Your personal Saunagus expert", style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 0.dp,
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFFF0F4F8)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ask me about today's shows, locations, or rituals.", fontSize = 14.sp, color = Color(0xFF003366))
                Spacer(Modifier.height(16.dp))
                TextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Which show is most relaxing?") },
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = { scope.launch { aiService.askAssistant(question, events, placesById) } },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && question.isNotBlank(),
                    elevation = ButtonDefaults.elevation(0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("ASK GEMINI", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        if (response.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("AI RESPONSE", style = MaterialTheme.typography.overline, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(response, style = MaterialTheme.typography.body1, lineHeight = 24.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Powered by Gemini 1.5 Flash", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
