package com.psrpis.watchporn

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WatchPornPsPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WatchPornPs())
    }
}
