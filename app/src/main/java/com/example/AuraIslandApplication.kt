package com.example

import android.app.Application
import com.example.core.DragAndGoResolver
import com.example.core.DynamicIslandCoordinator
import com.example.core.SystemHardwareMonitor
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AuraIslandApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var coordinator: DynamicIslandCoordinator
        private set

    lateinit var dragResolver: DragAndGoResolver
        private set

    lateinit var hardwareMonitor: SystemHardwareMonitor
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        coordinator = DynamicIslandCoordinator(this)
        dragResolver = DragAndGoResolver(this)
        hardwareMonitor = SystemHardwareMonitor(this, coordinator)
        hardwareMonitor.register()

        appScope.launch {
            settingsRepository.settingsFlow.collect { cfg ->
                coordinator.applySettings(cfg)
            }
        }
    }
}
