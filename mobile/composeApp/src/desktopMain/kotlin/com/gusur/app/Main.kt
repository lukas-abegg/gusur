package com.gusur.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.gusur.app.ui.SaunaGusApp
import com.gusur.app.util.initializePlatformFirebase

fun main() {
    initializePlatformFirebase()

    application {        Window(onCloseRequest = ::exitApplication, title = "Gusur - Saunagus Iceland") {
            SaunaGusApp()
        }
    }
}
