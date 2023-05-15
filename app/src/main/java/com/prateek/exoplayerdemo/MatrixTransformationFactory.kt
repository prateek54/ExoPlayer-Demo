package com.prateek.exoplayerdemo

import android.graphics.Matrix
import androidx.media3.common.C
import androidx.media3.effect.MatrixTransformation

internal object MatrixTransformationFactory {
    /**
     * Returns a [MatrixTransformation] that rescales the frames over the first [ ][.ZOOM_DURATION_SECONDS] seconds, such that the rectangle filled with the input frame increases
     * linearly in size from a single point to filling the full output frame.
     */
    fun createZoomInTransition(): MatrixTransformation {
        return MatrixTransformation { presentationTimeUs: Long -> calculateZoomInTransitionMatrix(presentationTimeUs) }
    }

    private const val ZOOM_DURATION_SECONDS = 2f
    private fun calculateZoomInTransitionMatrix(presentationTimeUs: Long): Matrix {
        val transformationMatrix = Matrix()
        val scale = Math.min(1f, presentationTimeUs / (C.MICROS_PER_SECOND * ZOOM_DURATION_SECONDS))
        transformationMatrix.postScale( /* sx = */scale,  /* sy = */scale)
        return transformationMatrix
    }



}