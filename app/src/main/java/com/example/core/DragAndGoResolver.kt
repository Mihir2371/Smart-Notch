package com.example.core

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Patterns
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.DragPayload
import com.example.model.DragPayloadType
import com.example.model.DragTargetApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DragAndGoResolver(private val context: Context) {

    // Bounded LRU cache at 48dp icon size to stay strictly under 80 MB RAM
    private val iconCache = LruCache<String, ImageBitmap>(28)
    private val iconSizePx = (48 * context.resources.displayMetrics.density).toInt().coerceIn(72, 144)

    fun classifyClipData(description: ClipDescription?, clipData: ClipData?): DragPayload {
        val mime = (0 until (description?.mimeTypeCount ?: 0))
            .mapNotNull { description?.getMimeType(it) }
            .firstOrNull { it != ClipDescription.MIMETYPE_TEXT_PLAIN }
            ?: description?.getMimeType(0)
            ?: "text/plain"

        val item = if (clipData != null && clipData.itemCount > 0) clipData.getItemAt(0) else null
        val text = item?.text?.toString()
        val uri = item?.uri

        val type = when {
            mime.startsWith("image/") -> DragPayloadType.IMAGE
            mime.startsWith("video/") -> DragPayloadType.VIDEO
            mime.startsWith("audio/") -> DragPayloadType.AUDIO
            mime == "application/pdf" -> DragPayloadType.PDF
            uri != null && mime == "*/*" -> DragPayloadType.FILE
            !text.isNullOrBlank() && Patterns.WEB_URL.matcher(text.trim()).matches() -> DragPayloadType.URL
            !text.isNullOrBlank() && (text.startsWith("geo:") || looksLikeAddress(text)) -> DragPayloadType.GEO
            !text.isNullOrBlank() && Patterns.PHONE.matcher(text.trim()).matches() -> DragPayloadType.PHONE
            uri != null -> DragPayloadType.FILE
            else -> DragPayloadType.TEXT
        }

        val effectiveMime = when (type) {
            DragPayloadType.IMAGE -> if (mime.startsWith("image/")) mime else "image/*"
            DragPayloadType.VIDEO -> "video/*"
            DragPayloadType.AUDIO -> "audio/*"
            DragPayloadType.PDF -> "application/pdf"
            DragPayloadType.URL, DragPayloadType.GEO, DragPayloadType.PHONE, DragPayloadType.TEXT -> "text/plain"
            DragPayloadType.FILE -> "*/*"
        }

        val label = description?.label?.toString()?.takeIf { it.isNotBlank() }
            ?: text?.take(28)
            ?: uri?.lastPathSegment
            ?: type.badgeLabel

        return DragPayload(
            type = type,
            mimeType = effectiveMime,
            title = label,
            textContent = text,
            uri = uri,
        )
    }

    suspend fun resolveTargets(payload: DragPayload): List<DragTargetApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val collected = LinkedHashMap<String, DragTargetApp>()

        fun addResolveInfos(infos: List<ResolveInfo>, action: String, mime: String) {
            for (info in infos) {
                val actInfo = info.activityInfo ?: continue
                val pkg = actInfo.packageName ?: continue
                if (pkg == context.packageName) continue
                if (collected.containsKey(pkg)) continue

                val label = info.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg
                val icon = loadCachedIcon(pm, info, pkg)
                collected[pkg] = DragTargetApp(
                    packageName = pkg,
                    activityName = actInfo.name,
                    label = label,
                    iconBitmap = icon,
                    intentAction = action,
                    resolvedMimeType = mime,
                )
            }
        }

        // 1. Specific VIEW intent targets (e.g., Browsers for URLs, Maps for Geo, Dialer for Phone, Viewers for Image/PDF)
        when (payload.type) {
            DragPayloadType.URL -> {
                val url = normalizeUrl(payload.textContent ?: "https://example.com")
                val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                addResolveInfos(queryActivitiesCompat(pm, viewIntent), Intent.ACTION_VIEW, "text/plain")
            }
            DragPayloadType.GEO -> {
                val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(payload.textContent ?: ""))
                val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                addResolveInfos(queryActivitiesCompat(pm, geoIntent), Intent.ACTION_VIEW, "text/plain")
            }
            DragPayloadType.PHONE -> {
                val telUri = Uri.parse("tel:" + Uri.encode(payload.textContent ?: ""))
                val telIntent = Intent(Intent.ACTION_DIAL, telUri)
                addResolveInfos(queryActivitiesCompat(pm, telIntent), Intent.ACTION_DIAL, "text/plain")
            }
            DragPayloadType.IMAGE, DragPayloadType.PDF, DragPayloadType.VIDEO, DragPayloadType.AUDIO -> {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(payload.uri ?: Uri.parse("content://sample"), payload.mimeType)
                }
                addResolveInfos(queryActivitiesCompat(pm, viewIntent), Intent.ACTION_VIEW, payload.mimeType)
            }
            else -> Unit
        }

        // 2. Universal ACTION_SEND targets for the exact MIME type (Photos, Messages, Mail, Drive, Notes, etc.)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = payload.mimeType
        }
        addResolveInfos(queryActivitiesCompat(pm, sendIntent), Intent.ACTION_SEND, payload.mimeType)

        collected.values.take(24)
    }

    fun dispatchDrop(target: DragTargetApp, payload: DragPayload): Result<String> {
        return runCatching {
            val intent = when (target.intentAction) {
                Intent.ACTION_VIEW -> {
                    val dataUri = when (payload.type) {
                        DragPayloadType.URL -> Uri.parse(normalizeUrl(payload.textContent ?: "https://example.com"))
                        DragPayloadType.GEO -> Uri.parse("geo:0,0?q=" + Uri.encode(payload.textContent ?: ""))
                        else -> payload.uri
                    }
                    Intent(Intent.ACTION_VIEW).apply {
                        if (dataUri != null && (payload.type == DragPayloadType.IMAGE || payload.type == DragPayloadType.PDF || payload.type == DragPayloadType.VIDEO || payload.type == DragPayloadType.AUDIO)) {
                            setDataAndType(dataUri, payload.mimeType)
                        } else if (dataUri != null) {
                            data = dataUri
                        }
                    }
                }
                Intent.ACTION_DIAL -> {
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(payload.textContent ?: "")))
                }
                else -> {
                    Intent(Intent.ACTION_SEND).apply {
                        type = target.resolvedMimeType
                        if (payload.uri != null) {
                            putExtra(Intent.EXTRA_STREAM, payload.uri)
                            clipData = ClipData.newRawUri(payload.title, payload.uri)
                        }
                        if (!payload.textContent.isNullOrBlank()) {
                            putExtra(Intent.EXTRA_TEXT, payload.textContent)
                        }
                    }
                }
            }.apply {
                setClassName(target.packageName, target.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            "Opened in ${target.label}"
        }
    }

    private fun queryActivitiesCompat(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    private fun loadCachedIcon(pm: PackageManager, info: ResolveInfo, pkg: String): ImageBitmap? {
        iconCache.get(pkg)?.let { return it }
        return runCatching {
            val drawable = info.loadIcon(pm)
            val bmp = drawable.toBitmap(width = iconSizePx, height = iconSizePx)
            val img = bmp.asImageBitmap()
            iconCache.put(pkg, img)
            img
        }.getOrNull()
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private fun looksLikeAddress(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("pkwy") || lower.contains("street") || lower.contains("ave") ||
            lower.contains("blvd") || lower.contains("road") || lower.contains("mountain view")
    }
}
