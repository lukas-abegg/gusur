package com.gusur.app.util

/**
 * On iOS, Firebase is initialized in Swift via FirebaseApp.configure() in the AppDelegate.
 * The Compose framework gets its FirebaseApp.app() reference through the gitlive bridge,
 * so this Kotlin-side init is a no-op.
 */
actual fun initializePlatformFirebase() {
    // intentionally empty — see iosApp/iosApp/GusurApp.swift
}
