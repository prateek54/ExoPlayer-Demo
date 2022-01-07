package com.prateek.exoplayerdemo

import android.os.Bundle
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), Player.Listener {
    private var qualityPopUp: PopupMenu?=null
    private var player: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var trackSelector:DefaultTrackSelector?=null
    var qualityList = ArrayList<Pair<String, TrackSelectionOverrides.Builder>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initListener()
    }

    private fun initListener() {
        exo_quality.setOnClickListener {
            qualityPopUp?.show()
        }
    }


    private fun initPlayer() {
        trackSelector = DefaultTrackSelector(/* context= */this, AdaptiveTrackSelection.Factory())
        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .build()
        player?.playWhenReady = true
        player_exo.player = player
        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        val drmConfig =
            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                .setLicenseUri("https://proxy.uat.widevine.com/proxy?provider=widevine_test")
        val mediaItem = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears.mpd")
            .setDrmConfiguration(drmConfig.build())
        val mediaSource =
            DashMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem.build())
        player?.setMediaSource(mediaSource)
        player?.seekTo(playbackPosition)
        player?.playWhenReady = playWhenReady
        player?.addListener(this)
        player?.prepare()

    }

    private fun setUpQualityList() {
        qualityPopUp = PopupMenu(this, exo_quality)
        qualityList.let {
            for ((i, videoQuality) in it.withIndex()) {
                qualityPopUp?.menu?.add(0, i, 0, videoQuality.first)
            }
        }
        qualityPopUp?.setOnMenuItemClickListener { menuItem ->
            qualityList[menuItem.itemId].let {
                trackSelector!!.setParameters(
                    trackSelector!!.getParameters()
                        .buildUpon()
                        .setTrackSelectionOverrides(it.second.build())
                        .setTunnelingEnabled(true)
                        .build())
            }
            true
        }
    }

    override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
        println("TRACK CHANGED")
        println(tracksInfo.trackGroupInfos)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState==Player.STATE_READY){
             trackSelector?.generateQualityList()?.let {
                 qualityList = it
                 setUpQualityList()
             }
        }
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