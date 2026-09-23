package com.geoalarm.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.geoalarm.app.AppContainer
import com.geoalarm.app.GeoAlarmApp

/** Minimal generic factory: avoids hand-writing a Factory class per screen ViewModel. */
class ContainerViewModelFactory(
    private val container: AppContainer,
    private val create: (AppContainer) -> ViewModel
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create(container) as T
}

@Composable
fun <T : ViewModel> rememberContainerViewModel(create: (AppContainer) -> T): T {
    val context = LocalContext.current
    val container = (context.applicationContext as GeoAlarmApp).container
    @Suppress("UNCHECKED_CAST")
    return viewModel(factory = ContainerViewModelFactory(container) { create(it) })
}
