import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// ---- Version is derived from the repository-root VERSION file (single source of truth). ----
val versionFile = rootProject.projectDir.parentFile.resolve("VERSION")
val appVersionName = if (versionFile.exists()) versionFile.readText().trim() else "0.0.0"
val versionParts = appVersionName.split(".").map { it.toIntOrNull() ?: 0 }
val appVersionCode = (versionParts.getOrElse(0) { 0 } * 10_000) +
    (versionParts.getOrElse(1) { 0 } * 100) +
    versionParts.getOrElse(2) { 0 }

// ---- Optional map/geocoder configuration from local.properties (see local.properties.example). ----
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
fun localProp(key: String, default: String): String = (localProps.getProperty(key) ?: default)

android {
    namespace = "com.geoalarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.geoalarm.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "TILE_URL_TEMPLATE",
            "\"${localProp("GEOALARM_TILE_URL_TEMPLATE", "https://tile.openstreetmap.org/{z}/{x}/{y}.png")}\""
        )
        buildConfigField(
            "String", "TILE_ATTRIBUTION",
            "\"${localProp("GEOALARM_TILE_ATTRIBUTION", "\u00A9 OpenStreetMap contributors")}\""
        )
        buildConfigField(
            "String", "GEOCODER_BASE_URL",
            "\"${localProp("GEOALARM_GEOCODER_BASE_URL", "https://nominatim.openstreetmap.org")}\""
        )
        buildConfigField(
            "String", "GEOCODER_USER_AGENT",
            "\"${localProp("GEOALARM_GEOCODER_USER_AGENT", "GeoAlarm/$appVersionName (no contact configured)")}\""
        )
    }

    signingConfigs {
        // Release signing is intentionally NOT configured here.
        // Never commit a keystore or its passwords to the repository.
        // See docs/android-setup.md#release-signing for how to sign a release build
        // locally (via ~/.gradle/gradle.properties or environment variables) or in CI
        // (via GitHub Actions secrets).
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No signingConfig assigned: `./gradlew assembleRelease` produces an UNSIGNED apk
            // until you configure signing per docs/android-setup.md. This is intentional --
            // we never claim a release build is production-signed unless it actually is.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Local persistence (Room).
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Background work for periodic maintenance tasks (e.g. history pruning).
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Location.
    implementation("androidx.core:core-location-altitude:1.0.0-rc01")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Map: MapLibre, an open-source (BSD-2) fork of the Mapbox Android SDK,
    // used here with OpenStreetMap-compatible raster tiles. See docs/map-provider.md.
    implementation("org.maplibre.gl:android-sdk:11.6.1")

    // Networking for search/geocoding only (never used for alarm monitoring).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // DataStore for lightweight settings (theme, default sound, etc).
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("app.cash.turbine:turbine:1.2.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
