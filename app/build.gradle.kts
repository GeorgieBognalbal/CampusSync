import java.util.Properties

plugins {
    id("com.android.application")
}

// Server address and key for AnnouncementSyncWorker, read from local.properties
// (that file is not committed, so each teammate can point at their own server).
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val apiBaseUrl = localProps.getProperty("campussync.apiBaseUrl", "http://localhost:5000")
val apiKey = localProps.getProperty("campussync.apiKey", "dev-key-change-me")

android {
    namespace = "com.example.campussync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.campussync"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.room:room-runtime:2.7.2")
    annotationProcessor("androidx.room:room-compiler:2.7.2")
    implementation("androidx.work:work-runtime:2.10.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
