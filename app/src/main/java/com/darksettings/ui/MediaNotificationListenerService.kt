package com.neoconfigurator.ui

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService

class MediaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    companion object {
        fun getActiveController(context: android.content.Context): MediaController? {
            return try {
                val mediaSessionManager = context.getSystemService(android.content.Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
                val controllers = mediaSessionManager.getActiveSessions(componentName)
                controllers?.firstOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }
}
