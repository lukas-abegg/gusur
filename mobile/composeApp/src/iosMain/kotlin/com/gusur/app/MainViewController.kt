package com.gusur.app

import androidx.compose.ui.window.ComposeUIViewController
import com.gusur.app.ui.SaunaGusApp

/**
 * Exported entry point for the iOS host. Swift code calls
 * `MainViewControllerKt.MainViewController()` (the K/N-mangled name) to obtain a
 * UIViewController that hosts the Compose Multiplatform UI.
 */
fun MainViewController() = ComposeUIViewController {
    SaunaGusApp()
}
