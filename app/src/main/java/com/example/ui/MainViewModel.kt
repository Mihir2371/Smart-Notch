package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AuraIslandApplication
import com.example.core.DragAndGoResolver
import com.example.core.DynamicIslandCoordinator
import com.example.core.IslandSnapshot
import com.example.core.SampleAssetProvider
import com.example.data.SettingsRepository
import com.example.model.DragPayload
import com.example.model.DragPayloadType
import com.example.model.IslandSettings
import com.example.model.TelemetryStats
import com.example.service.IslandOverlayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val auraApp = application as AuraIslandApplication
    val coordinator: DynamicIslandCoordinator = auraApp.coordinator
    val dragResolver: DragAndGoResolver = auraApp.dragResolver
    private val settingsRepo: SettingsRepository = auraApp.settingsRepository

    val snapshot: StateFlow<IslandSnapshot> = coordinator.snapshot
    val settings: StateFlow<IslandSettings> = coordinator.settings
    val telemetry: StateFlow<TelemetryStats> = coordinator.telemetry

    private val _sampleImageUri = MutableStateFlow<Uri?>(null)
    val sampleImageUri: StateFlow<Uri?> = _sampleImageUri.asStateFlow()

    private val _samplePdfUri = MutableStateFlow<Uri?>(null)
    val samplePdfUri: StateFlow<Uri?> = _samplePdfUri.asStateFlow()

    private val _pickedPhotoUri = MutableStateFlow<Uri?>(null)
    val pickedPhotoUri: StateFlow<Uri?> = _pickedPhotoUri.asStateFlow()

    init {
        viewModelScope.launch {
            _sampleImageUri.value = SampleAssetProvider.getSampleImageUri(application)
            _samplePdfUri.value = SampleAssetProvider.getSamplePdfUri(application)
        }
        coordinator.refreshTelemetry()
    }

    fun setPickedPhotoUri(uri: Uri?) {
        _pickedPhotoUri.value = uri
    }

    fun refreshTelemetry() {
        coordinator.refreshTelemetry()
    }

    fun toggleOverlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(overlayEnabled = enabled) }
        }
        if (enabled) {
            IslandOverlayService.start(auraApp)
        } else {
            IslandOverlayService.stop(auraApp)
        }
        coordinator.refreshTelemetry()
    }

    fun updateSettings(transform: (IslandSettings) -> IslandSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            settingsRepo.resetDefaults()
            IslandOverlayService.stop(auraApp)
        }
    }

    fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val mime = intent.type ?: "text/plain"
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        @Suppress("DEPRECATION")
        val sharedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        val payloadType = when {
            mime.startsWith("image/") -> DragPayloadType.IMAGE
            mime == "application/pdf" -> DragPayloadType.PDF
            !sharedText.isNullOrBlank() && (sharedText.startsWith("http://") || sharedText.startsWith("https://")) -> DragPayloadType.URL
            sharedUri != null -> DragPayloadType.FILE
            else -> DragPayloadType.TEXT
        }

        val payload = DragPayload(
            type = payloadType,
            mimeType = mime,
            title = sharedText?.take(28) ?: sharedUri?.lastPathSegment ?: "Shared Item",
            textContent = sharedText,
            uri = sharedUri,
        )
        viewModelScope.launch {
            val targets = dragResolver.resolveTargets(payload)
            coordinator.onDragStarted(payload, targets)
            coordinator.onDragHoverIsland(true)
        }
    }
}
