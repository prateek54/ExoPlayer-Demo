package com.prateek.exoplayerdemo

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.effect.RgbFilter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.TransformationException
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.TransformationResult
import androidx.media3.transformer.Transformer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), Player.Listener, Transformer.Listener {
    private var inputPlayer: ExoPlayer? = null
    private var outputPlayer: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var transformer: Transformer? = null
    private var filePath: File? = null
//    private val videoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/portrait_avc_aac.mp4"
    private val videoUrl = "https://bitdash-a.akamaihd.net/content/MI201109210084_1/m3u8s/f08e80da-bf1d-4e3d-8899-f0f6155f6efa.m3u8"
    private val fileName = "MediaFileTrans.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_transform_video.setOnClickListener {
            requestTransformerPermission()
        }
    }

    private fun initInputPlayer() {
        inputPlayer = ExoPlayer.Builder(this).build()
        inputPlayer?.playWhenReady = true
        player_exo_input.player = inputPlayer
        val mediaItem = MediaItem.fromUri(videoUrl)
        inputPlayer?.setMediaItem(mediaItem)
        inputPlayer?.seekTo(playbackPosition)
        inputPlayer?.playWhenReady = playWhenReady
        inputPlayer?.prepare()
    }

    private fun initOutputPlayer() {
        outputPlayer = ExoPlayer.Builder(this).build()
        outputPlayer?.playWhenReady = true
        player_exo_output.player = outputPlayer
        val mediaItem = MediaItem.fromUri("file://$filePath")
        outputPlayer?.setMediaItem(mediaItem)
        outputPlayer?.prepare()
    }

    private fun releasePlayer() {
        inputPlayer?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            inputPlayer = null
        }
        outputPlayer?.release()
        outputPlayer = null
    }


    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initInputPlayer()
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
        player_exo_input.onResume()
    }

    override fun onPause() {
        super.onPause()
        player_exo_input.onPause()
    }


    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun requestTransformerPermission() {
        if (Util.SDK_INT < 23) {
            return
        }
        if (checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(permission.READ_EXTERNAL_STORAGE),  /* requestCode = */0)
        } else {
            transformVideo()
        }
    }

    private fun transformVideo() {
        transformer = with(Transformer.Builder(this)) {
            addListener(this@MainActivity)
            if (btn_rotate.isChecked) {
                setTransformationRequest(
                    TransformationRequest.Builder()
                        .setRotationDegrees(90F).build()
                )
            }
            val effect = arrayListOf<Effect>()
            if (btn_grayscale.isChecked) {
                effect.add(RgbFilter.createGrayscaleFilter())
            }
            if (btn_zoom.isChecked) {
                effect.add(MatrixTransformationFactory.createZoomInTransition())
            }
            setVideoEffects(effect)
            setRemoveAudio(btn_audio.isChecked)
            build()
        }
        val inputMediaItem = MediaItem.Builder().apply {
            setUri(videoUrl)
            if (btn_trim.isChecked) {
                setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(0)
                        .setEndPositionMs(30_000)
                        .build()
                )
            }

        }.build()
        filePath = createExternalFile()
        transformer!!.startTransformation(inputMediaItem, filePath?.absolutePath!!)
    }

    @Throws(IOException::class)
    private fun createExternalFile(): File {
        val file = File(externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) { "Could not delete the previous transformer output file" }
        check(file.createNewFile()) { "Could not create the transformer output file" }
        return file
    }

    override fun onTransformationCompleted(
        inputMediaItem: MediaItem,
        transformationResult: TransformationResult
    ) {
        super.onTransformationCompleted(inputMediaItem, transformationResult)
        player_exo_output.visibility = View.VISIBLE
        initOutputPlayer()
        Toast.makeText(this, "Transformation Completed", Toast.LENGTH_SHORT).show()
    }

    override fun onTransformationError(
        inputMediaItem: MediaItem,
        exception: TransformationException
    ) {
        super.onTransformationError(inputMediaItem, exception)
        player_exo_output.visibility = View.GONE
        println("Transformation Failed${exception.errorCodeName}")
        Toast.makeText(this, "Transformation Failed${exception.errorCodeName}", Toast.LENGTH_SHORT).show()
    }

}