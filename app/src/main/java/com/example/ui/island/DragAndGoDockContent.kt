package com.example.ui.island

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DragAndGoState
import com.example.model.DragTargetApp
import com.example.model.EdgeScrollDirection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun DragAndGoDockContent(
    dragState: DragAndGoState,
    edgeScrollSpeed: Float,
    hapticsEnabled: Boolean,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current

    // Edge-hold continuous auto-scroll while the dragged item is held at either left or right edge
    LaunchedEffect(dragState.edgeScrollDirection, dragState.isHoveringIsland, edgeScrollSpeed) {
        if (!dragState.isHoveringIsland || dragState.edgeScrollDirection == EdgeScrollDirection.NONE) return@LaunchedEffect
        val deltaPx = (14f * edgeScrollSpeed) * when (dragState.edgeScrollDirection) {
            EdgeScrollDirection.LEFT -> -1f
            EdgeScrollDirection.RIGHT -> 1f
            EdgeScrollDirection.NONE -> 0f
        }
        while (true) {
            listState.scrollBy(deltaPx)
            delay(16L)
        }
    }

    // Subtle haptic tick whenever hovered app index changes
    LaunchedEffect(dragState.hoveredTargetIndex) {
        if (hapticsEnabled && dragState.hoveredTargetIndex != null) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentCyan.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = dragState.payload?.type?.badgeLabel?.uppercase() ?: "DRAG & GO",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentCyan,
                    )
                }
                Text(
                    text = dragState.payload?.title ?: "Drop onto an app",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(150.dp),
                )
            }
            Text(
                text = "${dragState.resolvedTargets.size} apps",
                style = MaterialTheme.typography.labelSmall,
                color = AccentEmerald,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
        ) {
            if (dragState.resolvedTargets.isEmpty()) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Resolving matching apps via PackageManager…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            } else {
                LazyRow(
                    state = listState,
                    modifier = Modifier.matchParentSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    itemsIndexed(
                        items = dragState.resolvedTargets,
                        key = { _, item -> item.packageName },
                    ) { index, app ->
                        DragTargetAppItem(
                            app = app,
                            isHovered = dragState.hoveredTargetIndex == index,
                        )
                    }
                }
            }

            // Left Edge-Hold Auto-Scroll Glow Indicator
            if (dragState.edgeScrollDirection == EdgeScrollDirection.LEFT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(36.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentCyan.copy(alpha = 0.45f), Color.Transparent),
                            ),
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Scrolling left",
                        tint = AccentCyan,
                    )
                }
            }

            // Right Edge-Hold Auto-Scroll Glow Indicator
            if (dragState.edgeScrollDirection == EdgeScrollDirection.RIGHT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(36.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, AccentCyan.copy(alpha = 0.45f)),
                            ),
                        ),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Scrolling right",
                        tint = AccentCyan,
                    )
                }
            }
        }
    }
}

@Composable
private fun DragTargetAppItem(
    app: DragTargetApp,
    isHovered: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "drag_app_scale",
    )

    Column(
        modifier = Modifier
            .width(62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isHovered) AccentCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                .then(
                    if (isHovered) {
                        Modifier.border(1.5.dp, AccentCyan, RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Apps,
                    contentDescription = app.label,
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (isHovered) AccentCyan else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Maps local drag pointer X inside the expanded Drag & Go dock to the matching app index
 * and edge-scroll direction (left/right 48dp hot-zones).
 */
fun computeDragPointerTargets(
    localX: Float,
    containerWidthPx: Float,
    density: Float,
    listState: LazyListState,
): Pair<Int?, EdgeScrollDirection> {
    if (containerWidthPx <= 0f) return null to EdgeScrollDirection.NONE
    val edgeZonePx = 48f * density
    val edgeDir = when {
        localX < edgeZonePx -> EdgeScrollDirection.LEFT
        localX > containerWidthPx - edgeZonePx -> EdgeScrollDirection.RIGHT
        else -> EdgeScrollDirection.NONE
    }

    val visibleItems = listState.layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null to edgeDir

    // Find closest visible item center to localX (accounting for container horizontal padding ~14dp)
    val rowLocalX = localX - (14f * density)
    var bestIndex: Int? = null
    var bestDist = Float.MAX_VALUE
    for (item in visibleItems) {
        val center = item.offset + item.size / 2f
        val dist = kotlin.math.abs(center - rowLocalX)
        if (dist < bestDist && dist <= item.size * 0.75f) {
            bestDist = dist
            bestIndex = item.index
        }
    }
    return bestIndex to edgeDir
}
