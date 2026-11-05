package com.psrpis.homehub

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HomeHubPlugin : Plugin() {
    override fun load(context: Context) {
        HomeHubPrefs.init(context)
        registerMainAPI(HomeHubProvider())

        // Eklentiler ekranındaki ayar (dişli) butonu
        openSettings = { ctx -> HomeHubSettings.show(ctx) }
    }
}
