package com.psrpis.milfnut

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class MilfNutPsPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(MilfNutPs())
    }
}
