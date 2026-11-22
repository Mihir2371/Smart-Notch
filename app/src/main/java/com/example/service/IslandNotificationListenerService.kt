package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.AuraIslandApplication
import com.example.model.LiveActivity
import com.example.model.NavDirection

class IslandNotificationListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            pushMediaUpdate(activeController)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            pushMediaUpdate(activeController)
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        bindBestMediaController(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        runCatching {
            val mgr = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return@runCatching
            mediaSessionManager = mgr
            val component = ComponentName(this, IslandNotificationListenerService::class.java)
            mgr.addOnActiveSessionsChangedListener(sessionsListener, component)
            bindBestMediaController(mgr.getActiveSessions(component))
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        runCatching {
            activeController?.unregisterCallback(controllerCallback)
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsListener)
        }
        activeController = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val notif = sbn.notification ?: return
        if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val app = application as? AuraIslandApplication ?: return
        val coordinator = app.coordinator
        val extras = notif.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val appLabel = runCatching {
            val info = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrElse { sbn.packageName }

        // 1. Progress notification -> File / App Download or Upload Live Activity
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        if (progressMax > 0 && progress >= 0) {
            val combined = "$title $text".lowercase()
            val isUpload = combined.contains("upload") || combined.contains("backing up") || combined.contains("sending")
            coordinator.updateFromSystemDownloadNotification(
                key = sbn.key,
                title = title.ifBlank { "Transfer" },
                sourceApp = appLabel,
                progress = progress,
                max = progressMax,
                isUpload = isUpload,
            )
            return
        }

        // 2. Call notifications
        if (notif.category == Notification.CATEGORY_CALL && title.isNotBlank()) {
            coordinator.triggerCall(incoming = true)
            return
        }

        // 3. Navigation notifications
        if (notif.category == Notification.CATEGORY_NAVIGATION && title.isNotBlank()) {
            val dir = when {
                text.contains("left", ignoreCase = true) -> NavDirection.LEFT
                text.contains("right", ignoreCase = true) -> NavDirection.RIGHT
                else -> NavDirection.STRAIGHT
            }
            coordinator.triggerNavigation()
            return
        }

        // 4. Ignore media-style notifications (handled natively via MediaSessionManager)
        if (notif.category == Notification.CATEGORY_TRANSPORT || extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            return
        }

        // 5. Standard messaging / system alerts
        if (title.isNotBlank() && text.isNotBlank() && !sbn.isOngoing) {
            coordinator.pushSystemNotification(
                packageName = sbn.packageName,
                appName = appLabel,
                title = title,
                message = text,
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val app = application as? AuraIslandApplication ?: return
        app.coordinator.dismissActivity("sys_download_${sbn.key}")
        app.coordinator.dismissActivity("sys_upload_${sbn.key}")
    }

    private fun bindBestMediaController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        val chosen = controllers?.firstOrNull {
            val state = it.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED
        } ?: controllers?.firstOrNull()

        activeController = chosen
        chosen?.registerCallback(controllerCallback)
        pushMediaUpdate(chosen)
    }

    private fun pushMediaUpdate(controller: MediaController?) {
        val app = application as? AuraIslandApplication ?: return
        if (controller == null) {
            app.coordinator.bindSystemMediaController(null, null, null, false, 0L, 0L, null)
            return
        }
        val metadata = controller.metadata
        val state = controller.playbackState
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 180_000L
        val pos = state?.position ?: 0L
        val playing = state?.state == PlaybackState.STATE_PLAYING

        // Downscale album art to <=96px to respect strict <80MB RAM budget
        val rawArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val smallArt = rawArt?.let {
            if (it.width > 96 || it.height > 96) Bitmap.createScaledBitmap(it, 96, 96, true) else it
        }

        app.coordinator.bindSystemMediaController(
            controller = controller,
            title = title,
            artist = artist,
            isPlaying = playing,
            positionMs = pos,
            durationMs = duration,
            artwork = smallArt,
        )
    }
}
