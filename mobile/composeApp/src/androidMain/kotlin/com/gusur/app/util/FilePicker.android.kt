package com.gusur.app.util

actual fun initializePlatformFirebase() {
    // Android Firebase init is performed in MainActivity.onCreate via FirebaseApp.initializeApp(this).
}

/**
 * Admin-only JSON import is currently desktop-only. Stubbed on Android to satisfy the expect/actual
 * contract. If you wire admin import on phones, replace with an ActivityResultContracts.OpenDocument
 * launcher (needs the active Activity, so route through a CompositionLocal of ActivityResultRegistry).
 */
actual fun pickJsonFile(onFilePicked: (String?) -> Unit) {
    onFilePicked(null)
}
