package com.prateek.exoplayerdemo.cast

import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import com.prateek.exoplayerdemo.R
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy

class ExpandedControlsActivity : ExpandedControllerActivity() {
    private var DEFAULT_COOKIE_MANAGER: CookieManager? = null

    init {
        DEFAULT_COOKIE_MANAGER = CookieManager()
        DEFAULT_COOKIE_MANAGER!!.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.expanded_controller, menu)
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER)
        }
    }
}