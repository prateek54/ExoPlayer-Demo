package com.prateek.exoplayerdemo.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.prateek.exoplayerdemo.R

/**
 * App id for receiver app with rudimentary support for DRM.
 *
 * <p>This app id is only suitable for ExoPlayer's Cast Demo app, and it is not intended for
 * production use. In order to use DRM, custom receiver apps should be used. For environments that
 * do not require DRM, the default receiver app should be used (see {@link
 * #APP_ID_DEFAULT_RECEIVER}).
 */
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(p0: Context): CastOptions {

        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.name)
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(p0.getString(R.string.app_id_def))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(p0: Context): MutableList<SessionProvider>? {
        return null
    }

}