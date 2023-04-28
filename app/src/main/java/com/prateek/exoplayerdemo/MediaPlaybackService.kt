package com.prateek.exoplayerdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.session.MediaSession.ControllerInfo
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MediaPlaybackService : MediaLibraryService() {
    private val librarySessionCallback = CustomMediaLibrarySessionCallback()

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private lateinit var customCommands: List<CommandButton>
    private var customLayout = ImmutableList.of<CommandButton>()

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "demo_session_notification_channel_id"
        private const val FWD = "FORWARD"
        private const val RWD = "REWIND"
    }

    override fun onCreate() {
        super.onCreate()
        customCommands =
            listOf(
                getCustomCommand(SessionCommand(RWD, Bundle.EMPTY)),
                getCustomCommand(SessionCommand(FWD, Bundle.EMPTY))
            )
        customLayout = ImmutableList.of(customCommands[0], customCommands[1])
        initializeSessionAndPlayer()
        setListener(MediaSessionServiceListener())
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        player.release()
        mediaLibrarySession.release()
        clearListener()
        super.onDestroy()
    }

    private inner class CustomMediaLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession, controller: ControllerInfo
        ): MediaSession.ConnectionResult {
            val connectionResult = super.onConnect(session, controller)
            val availableSessionCommands = connectionResult.availableSessionCommands.buildUpon()
            customCommands.forEach { commandButton ->
                // Add custom command to available session commands.
                commandButton.sessionCommand?.let { availableSessionCommands.add(it) }
            }
            return MediaSession.ConnectionResult.accept(
                availableSessionCommands.build(),
                connectionResult.availablePlayerCommands
            )
        }

        override fun onPostConnect(session: MediaSession, controller: ControllerInfo) {
            if (!customLayout.isEmpty() && controller.controllerVersion != 0) {
                // Let Media3 controller (for instance the MediaNotificationProvider) know about the custom
                // layout right after it connected.
                mediaLibrarySession.setCustomLayout(controller, customLayout)
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == RWD) {
                player.seekBack()
            } else if (customCommand.customAction == FWD) {
                player.seekForward()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val updatedMediaItems = mediaItems.map {
                it.buildUpon()
                    .setUri(it.mediaId)
                    .build()
            }.toMutableList()
            return Futures.immediateFuture(updatedMediaItems)
        }
    }

    private fun getCustomCommand(sessionCommand: SessionCommand): CommandButton {
        val isForward = sessionCommand.customAction == FWD
        return CommandButton.Builder()
            .setDisplayName(if (isForward) "Forward" else "Rewind")
            .setSessionCommand(sessionCommand)
            .setIconResId(if (isForward) R.drawable.ic_fast_forward_24 else R.drawable.ic_fast_rewind_24)
            .build()
    }

    private fun initializeSessionAndPlayer() {
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true).build()

        val sessionActivityPendingIntent = TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@MediaPlaybackService, MainActivity::class.java))

            val immutableFlag = if (Build.VERSION.SDK_INT >= 23) FLAG_IMMUTABLE else 0
            getPendingIntent(0, immutableFlag or FLAG_UPDATE_CURRENT)
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, librarySessionCallback)
            .setSessionActivity(sessionActivityPendingIntent).build()

        if (!customLayout.isEmpty()) {
            // Send custom layout to legacy session.
            mediaLibrarySession.setCustomLayout(customLayout)
        }
    }


    private inner class MediaSessionServiceListener : Listener {

        /**
         * This method is only required to be implemented on Android 12 or above when an attempt is made
         * by a media controller to resume playback when the {@link MediaSessionService} is in the
         * background.
         */
        override fun onForegroundServiceStartNotAllowedException() {
            val notificationManagerCompat =
                NotificationManagerCompat.from(this@MediaPlaybackService)
            ensureNotificationChannel(notificationManagerCompat)
            val pendingIntent = TaskStackBuilder.create(this@MediaPlaybackService).run {
                addNextIntent(Intent(this@MediaPlaybackService, MainActivity::class.java))

                val immutableFlag = if (Build.VERSION.SDK_INT >= 23) FLAG_IMMUTABLE else 0
                getPendingIntent(0, immutableFlag or FLAG_UPDATE_CURRENT)
            }
            val builder = NotificationCompat.Builder(this@MediaPlaybackService, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.media3_notification_small_icon).setContentTitle("Title")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText("Description")
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true)
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (Util.SDK_INT < 26 || notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManagerCompat.createNotificationChannel(channel)
    }
}
