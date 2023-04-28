package com.prateek.exoplayerdemo

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun releasePlayer() {
        player_exo.player = null
    }

    private fun initializeController() {
        controllerFuture =
            MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, MediaPlaybackService::class.java))
            )
                .buildAsync()
        controllerFuture.addListener({ setController() }, MoreExecutors.directExecutor())
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    private fun setController() {
        val controller = this.controller ?: return
        player_exo.player = controller
        val metadata = MediaMetadata.Builder()
            .setTitle("Dummy Video")
            .setDescription("Dummy Video .....")
            .setArtworkUri(Uri.parse("https://cdn.pixabay.com/photo/2014/10/09/13/14/video-481821_960_720.png"))
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId("https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8")
            .setMediaMetadata(metadata)
            .build()
        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }


    override fun onStart() {
        super.onStart()
        initializeController()
    }

    override fun onStop() {
        releasePlayer()
        releaseController()
        super.onStop()
    }

    override fun onResume() {
        player_exo.onResume()
        super.onResume()
    }

    override fun onPause() {
        player_exo.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}