package com.gusur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gusur.app.theme.GusurColors

private fun Modifier.formNav(focusManager: FocusManager, isLast: Boolean, onSubmit: () -> Unit): Modifier =
    this.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.Tab -> {
                focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                true
            }
            Key.Enter -> {
                if (isLast) onSubmit() else focusManager.moveFocus(FocusDirection.Next)
                true
            }
            else -> false
        }
    }

@Composable
fun LoginScreen(errorMessage: String?, onLogin: (String, String) -> Unit, onRegister: (String, String, String) -> Unit) {
    var isRegister by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    val handleAction = {
        focusManager.clearFocus()
        if (isRegister) {
            if (email.isNotBlank() && password.isNotBlank() && name.isNotBlank()) {
                onRegister(email, password, name)
            }
        } else {
            if (email.isNotBlank() && password.isNotBlank()) {
                onLogin(email, password)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2A0E04), Color(0xFF5D1F08), GusurColors.Ember)))) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 40.dp)) {
                Surface(shape = CircleShape, color = GusurColors.Paper, modifier = Modifier.size(80.dp)) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = GusurColors.Ember, modifier = Modifier.size(40.dp)) } }
                Spacer(Modifier.height(12.dp))
                Text("Gusur.", style = MaterialTheme.typography.h4, color = Color.White)
                Text(if (isRegister) "CREATE ACCOUNT" else "WELCOME BACK", style = MaterialTheme.typography.overline, color = Color.White.copy(alpha = 0.7f))
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Surface(color = GusurColors.Gold.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(errorMessage, color = GusurColors.Gold, modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (isRegister) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name", color = Color.White) },
                        modifier = Modifier.fillMaxWidth().formNav(focusManager, isLast = false, onSubmit = handleAction),
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    modifier = Modifier.fillMaxWidth().formNav(focusManager, isLast = false, onSubmit = handleAction),
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color.White) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().formNav(focusManager, isLast = true, onSubmit = handleAction),
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleAction() })
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { handleAction() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(backgroundColor = GusurColors.Paper)) { Text(if (isRegister) "SIGN UP" else "LOG IN", fontWeight = FontWeight.ExtraBold) }
                TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (isRegister) "Already have an account? Log in" else "Don't have an account? Sign up", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp) }
            }
        }
    }
}
