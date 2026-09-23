package com.geoalarm.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.geoalarm.app.data.repository.ThemeMode
import com.geoalarm.app.ui.Routes
import com.geoalarm.app.ui.activealarm.ActiveAlarmScreen
import com.geoalarm.app.ui.alarmedit.AlarmCreateScreen
import com.geoalarm.app.ui.alarms.MyAlarmsScreen
import com.geoalarm.app.ui.history.HistoryScreen
import com.geoalarm.app.ui.home.HomeScreen
import com.geoalarm.app.ui.map.MapPickDestinationScreen
import com.geoalarm.app.ui.onboarding.OnboardingScreen
import com.geoalarm.app.ui.permissions.PermissionFlowScreen
import com.geoalarm.app.ui.settings.SettingsScreen
import com.geoalarm.app.ui.theme.GeoAlarmTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as GeoAlarmApp).container

        setContent {
            val theme by container.settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            GeoAlarmTheme(theme) {
                val navController = rememberNavController()
                var onboardingChecked by remember { mutableStateOf(false) }
                var onboardingDone by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScopeSafe()

                LaunchedEffect(Unit) {
                    onboardingDone = kotlinx.coroutines.flow.first(container.settingsRepository.onboardingComplete)
                    onboardingChecked = true
                }

                if (!onboardingChecked) return@GeoAlarmTheme

                NavHost(navController = navController, startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            onDone = {
                                scope.launch { container.settingsRepository.setOnboardingComplete(true) }
                                navController.navigate(Routes.PERMISSIONS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                            },
                            onSkip = {
                                scope.launch { container.settingsRepository.setOnboardingComplete(true) }
                                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                            }
                        )
                    }
                    composable(Routes.PERMISSIONS) {
                        PermissionFlowScreen(
                            onFinished = { navController.navigate(Routes.HOME) { popUpTo(Routes.PERMISSIONS) { inclusive = true } } }
                        )
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onChooseDestination = { navController.navigate(Routes.MAP_PICK_DESTINATION) },
                            onMyAlarms = { navController.navigate(Routes.MY_ALARMS) },
                            onHistory = { navController.navigate(Routes.HISTORY) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                            onViewActiveAlarm = { id -> navController.navigate(Routes.activeAlarm(id)) }
                        )
                    }
                    composable(Routes.MAP_PICK_DESTINATION) {
                        MapPickDestinationScreen(
                            onDestinationConfirmed = { label, lat, lon ->
                                navController.navigate(Routes.alarmCreate(lat, lon, label))
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        Routes.ALARM_CREATE,
                        arguments = listOf(
                            navArgument("lat") { type = NavType.StringType },
                            navArgument("lon") { type = NavType.StringType },
                            navArgument("label") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                        val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
                        val label = backStackEntry.arguments?.getString("label")
                            ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Destination"
                        AlarmCreateScreen(
                            destinationName = label,
                            latitude = lat,
                            longitude = lon,
                            onCreated = { id ->
                                navController.navigate(Routes.activeAlarm(id)) {
                                    popUpTo(Routes.HOME)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.MY_ALARMS) {
                        MyAlarmsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenAlarm = { id -> navController.navigate(Routes.activeAlarm(id)) }
                        )
                    }
                    composable(
                        Routes.ACTIVE_ALARM,
                        arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
                        ActiveAlarmScreen(
                            alarmId = alarmId,
                            onBack = { navController.popBackStack() },
                            onCancelled = { navController.popBackStack(Routes.HOME, inclusive = false) }
                        )
                    }
                    composable(Routes.HISTORY) { HistoryScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
