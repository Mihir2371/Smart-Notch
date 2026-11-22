package com.example.ui.screens

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DragAndGoState
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentViolet
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.IslandBlack
import com.example.ui.theme.SurfaceCarbon
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DragAndGoStudioScreen(
    dragState: DragAndGoState,
    sampleImageUri: Uri?,
    samplePdfUri: Uri?,
    pickedPhotoUri: Uri?,
    onPhotoPicked: (Uri?) -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> onPhotoPicked(uri) },
    )

    val activeImageUri = pickedPhotoUri ?: sampleImageUri

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCarbon),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "OriginOS Drag & Go",
                            style = MaterialTheme.typography.titleLarge,
                            color = AccentCyan,
                        )
                        Text(
                            text = "1. Long-press & drag any item below.\n" +
                                "2. Pull it near the Dynamic Island above — PackageManager resolves all apps supporting that item.\n" +
                                "3. Hold at the left or right edge of the expanded Island to auto-scroll the row.\n" +
                                "4. Drop onto any app icon to launch it immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        if (!dragState.lastActionStatus.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentEmerald.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = dragState.lastActionStatus,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AccentEmerald,
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Draggable Photo (Image/*)")
                Spacer(modifier = Modifier.height(8.dp))
                DraggablePhotoCard(
                    imageUri = activeImageUri,
                    isCustomPicked = pickedPhotoUri != null,
                    onPickPhoto = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
            }

            item {
                SectionHeader("Draggable Text, Links, Locations & Files")
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DraggablePayloadCard(
                        icon = Icons.Filled.Link,
                        title = "Web Link",
                        subtitle = "https://developer.android.com",
                        accent = AccentCyan,
                        tag = "drag_card_url",
                        clipDataProvider = {
                            ClipData.newPlainText("Web Link", "https://developer.android.com")
                        },
                    )
                    DraggablePayloadCard(
                        icon = Icons.Filled.LocationOn,
                        title = "Map Address",
                        subtitle = "1600 Amphitheatre Pkwy, Mountain View, CA",
                        accent = AccentEmerald,
                        tag = "drag_card_geo",
                        clipDataProvider = {
                            ClipData.newPlainText("Map Address", "1600 Amphitheatre Pkwy, Mountain View, CA")
                        },
                    )
                    DraggablePayloadCard(
                        icon = Icons.Filled.Notes,
                        title = "Note / Message Snippet",
                        subtitle = "Meet at 5 PM near the waterfront pavilion!",
                        accent = AccentViolet,
                        tag = "drag_card_text",
                        clipDataProvider = {
                            ClipData.newPlainText("Note Snippet", "Meet at 5 PM near the waterfront pavilion!")
                        },
                    )
                    DraggablePayloadCard(
                        icon = Icons.Filled.Phone,
                        title = "Phone Number",
                        subtitle = "+1 650 555 0199",
                        accent = AccentAmber,
                        tag = "drag_card_phone",
                        clipDataProvider = {
                            ClipData.newPlainText("Phone Number", "+1 650 555 0199")
                        },
                    )
                    DraggablePayloadCard(
                        icon = Icons.Filled.PictureAsPdf,
                        title = "PDF Document",
                        subtitle = "Aura_Island_Guide.pdf",
                        accent = AccentBlue,
                        tag = "drag_card_pdf",
                        clipDataProvider = {
                            if (samplePdfUri != null) {
                                ClipData(
                                    ClipDescription("Aura_Island_Guide.pdf", arrayOf("application/pdf")),
                                    ClipData.Item(samplePdfUri),
                                )
                            } else {
                                ClipData.newPlainText("Aura_Island_Guide.pdf", "Aura Island Guide")
                            }
                        },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggablePhotoCard(
    imageUri: Uri?,
    isCustomPicked: Boolean,
    onPickPhoto: () -> Unit,
) {
    val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceCarbon)
            .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(14.dp))
                .dragAndDropSource {
                    detectTapGestures(
                        onLongPress = {
                            val clip = if (imageUri != null) {
                                ClipData(
                                    ClipDescription("Aurora Photo", arrayOf("image/*")),
                                    ClipData.Item(imageUri),
                                )
                            } else {
                                ClipData.newPlainText("Aurora Photo", "Aurora Borealis")
                            }
                            startTransfer(DragAndDropTransferData(clipData = clip, flags = flags))
                        },
                    )
                }
                .testTag("drag_card_photo"),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_sample_photo_1795341179984),
                contentDescription = "Draggable sample photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IslandBlack.copy(alpha = 0.78f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isCustomPicked) "Long-press & drag picked photo" else "Long-press & drag Aurora.jpg",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                )
            }
        }

        Button(
            onClick = onPickPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_photo_drag_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceElevated,
                contentColor = AccentCyan,
            ),
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pick Any Photo (Android Photo Picker)")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DraggablePayloadCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    tag: String,
    clipDataProvider: () -> ClipData,
) {
    val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCarbon)
            .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
            .dragAndDropSource {
                detectTapGestures(
                    onLongPress = {
                        startTransfer(DragAndDropTransferData(clipData = clipDataProvider(), flags = flags))
                    },
                )
            }
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Filled.DragIndicator,
            contentDescription = "Long press to drag",
            tint = TextSecondary,
        )
    }
}
