pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MapLibre's Maven repository (OpenStreetMap-compatible map renderer).
        maven { url = uri("https://repo.maplibre.org/releases") }
    }
}

rootProject.name = "GeoAlarm"
include(":app")
