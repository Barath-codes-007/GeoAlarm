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
        mavenCentral() // Also resolves org.maplibre.gl:android-sdk — no extra repo needed.
    }
}

rootProject.name = "GeoAlarm"
include(":app")
