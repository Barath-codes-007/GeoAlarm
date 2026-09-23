package com.geoalarm.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val Context.dataStore by preferencesDataStore(name = "geoalarm_settings")

/** App-wide, non-alarm-specific preferences (Section 29 dark mode, Section 15A reverse-geocode opt-out). */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val reverseGeocodeTappedPoints = booleanPreferencesKey("reverse_geocode_tapped_points")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.theme] = mode.name }
    }

    /** Privacy setting (see web/privacy.html): whether tapping the map looks up an address,
     * which sends the tapped coordinates to the configured geocoder. Defaults to on but is
     * a single, clearly documented toggle in Settings. */
    val reverseGeocodeTappedPoints: Flow<Boolean> = context.dataStore.data.map { it[Keys.reverseGeocodeTappedPoints] ?: true }

    suspend fun setReverseGeocodeTappedPoints(enabled: Boolean) {
        context.dataStore.edit { it[Keys.reverseGeocodeTappedPoints] = enabled }
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingComplete] ?: false }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.onboardingComplete] = complete }
    }
}
