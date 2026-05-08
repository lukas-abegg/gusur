plugins {
    // Versions that are known to play well together
    kotlin("multiplatform") version "1.9.23" apply false
    kotlin("plugin.serialization") version "1.9.23" apply false
    id("org.jetbrains.compose") version "1.6.1" apply false
    id("com.android.application") version "8.12.0" apply false
    id("com.android.library") version "8.12.0" apply false
    id("com.google.gms.google-services") version "4.4.1" apply false
}
