package com.prateek.exoplayerdemo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.util.Util
import com.prateek.exoplayerdemo.databinding.ActivityMainBinding
import com.prateek.exoplayerdemo.manager.DemoUtil
import com.prateek.exoplayerdemo.manager.DownloadTracker

class OfflinePlayerActivity : AppCompatActivity(), Player.Listener {
    private var player: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private lateinit var binding: ActivityMainBinding
    private var downloadTracker: DownloadTracker? = null

    companion object {
        const val VIDEO_URL =
            "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"

        fun getIntent(context: Context): Intent {
            return Intent(context, OfflinePlayerActivity::class.java)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        downloadTracker = DemoUtil.getDownloadTracker(this)
        binding.download.visibility = View.GONE
        binding.playOffline.visibility = View.GONE
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        player?.playWhenReady = true
        binding.playerExo.player = player

        val mediaSource = downloadTracker?.getDownloadRequest(Uri.parse(VIDEO_URL))!!.let {
            DownloadHelper.createMediaSource(
                it,
                DemoUtil.getDataSourceFactory(this)
            )
        }
        player?.setMediaSource(mediaSource)
        player?.seekTo(playbackPosition)
        player?.playWhenReady = playWhenReady
        player?.prepare()

    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            player = null
        }
    }


    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24) {
            initPlayer()
        }
    }
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}