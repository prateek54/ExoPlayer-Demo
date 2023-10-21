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
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
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
            transformer?.let {
                printCurrentProgress()
            }?: kotlin.run {
                transformVideo()

            }
        }
    }

    private fun printCurrentProgress() {
        val progressLogger = ProgressHolder()
        if (transformer?.getProgress(progressLogger) == Transformer.PROGRESS_STATE_AVAILABLE) {
            println(progressLogger.progress)
        }
    }

    private fun transformVideo() {
        val effect = arrayListOf<Effect>()
        if (btn_grayscale.isChecked) {
            effect.add(RgbFilter.createGrayscaleFilter())
        }
        if (btn_zoom.isChecked) {
            effect.add(MatrixTransformationFactory.createZoomInTransition())
        }
        if (btn_rotate.isChecked) {
            effect.add(ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(90F)
                .build())
        }
        transformer = with(Transformer.Builder(this)) {
            addListener(this@MainActivity)
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
        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem).apply {
            setRemoveAudio(btn_audio.isChecked)
            setEffects(Effects(mutableListOf(), effect))
        }

        filePath = createExternalFile()
        println("Transformation Started")
        transformer!!.start(editedMediaItem.build(), filePath?.absolutePath!!)
    }

    @Throws(IOException::class)
    private fun createExternalFile(): File {
        val file = File(externalCacheDir, fileName)
        check(!(file.exists() && !file.delete())) { "Could not delete the previous transformer output file" }
        check(file.createNewFile()) { "Could not create the transformer output file" }
        return file
    }

    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
        super.onCompleted(composition, exportResult)
        player_exo_output.visibility = View.VISIBLE
        initOutputPlayer()
        println("Transformation Completed")
        Toast.makeText(this, "Transformation Completed", Toast.LENGTH_SHORT).show()
    }



    override fun onError(
        composition: Composition,
        exportResult: ExportResult,
        exportException: ExportException
    ) {
        super.onError(composition, exportResult, exportException)
        player_exo_output.visibility = View.GONE
        println("Transformation Failed${exportException.errorCodeName}")
        Toast.makeText(this, "Transformation Failed${exportException.errorCodeName}", Toast.LENGTH_SHORT).show()
    }
}