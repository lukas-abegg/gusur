package com.gusur.app.util

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize

actual fun initializePlatformFirebase() {
    try {
        // Use the Multiplatform FirebaseOptions which includes apiKey
        dev.gitlive.firebase.Firebase.initialize(
            options = FirebaseOptions(
                applicationId = "1:685760890513:android:5fad46a9b9bdcc7834e130",
                apiKey = "AIzaSyARyXedp_4HR5Ke1VHzlFHOCXWqmRiKstM",
                projectId = "gusur-57e95",
                storageBucket = "gusur-57e95.firebasestorage.app",
                databaseUrl = "https://gusur-57e95-default-rtdb.europe-west1.firebasedatabase.app/"
            )
        )
        println("Desktop Firebase Init: Multiplatform JVM Success")
    } catch (e: Exception) {
        println("Desktop Firebase Init Error: ${e.message}")
    }
}
