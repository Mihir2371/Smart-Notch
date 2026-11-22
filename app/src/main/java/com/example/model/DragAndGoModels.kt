package com.example.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

enum class DragPayloadType(val badgeLabel: String) {
    IMAGE("Image"),
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("Document"),
    URL("Web Link"),
    GEO("Location"),
    PHONE("Phone"),
    TEXT("Text"),
    FILE("File"),
}

enum class EdgeScrollDirection {
    NONE,
    LEFT,
    RIGHT,
}

@Immutable
data class DragPayload(
    val type: DragPayloadType,
    val mimeType: String,
    val title: String,
    val textContent: String? = null,
    val uri: Uri? = null,
)

@Immutable
data class DragTargetApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val iconBitmap: ImageBitmap?,
    val intentAction: String,
    val resolvedMimeType: String,
)

@Immutable
data class DragAndGoState(
    val isDragActive: Boolean = false,
    val isHoveringIsland: Boolean = false,
    val payload: DragPayload? = null,
    val resolvedTargets: List<DragTargetApp> = emptyList(),
    val hoveredTargetIndex: Int? = null,
    val edgeScrollDirection: EdgeScrollDirection = EdgeScrollDirection.NONE,
    val lastActionStatus: String? = null,
)
