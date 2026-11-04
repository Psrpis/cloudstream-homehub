package com.psrpis.homehub

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class HomeHubPlugin : Plugin() {
    override fun load(context: Context) {
        HomeHubPrefs.init(context)
        registerMainAPI(HomeHubProvider())

        // Eklentiler ekranındaki ayar (dişli) butonu
        openSettings = { ctx -> showSettings(ctx) }
    }

    private fun showSettings(ctx: Context) {
        val names = allApis()
            .filter { it.hasMainPage && it.name != "HomeHub" }
            .map { it.name }
            .distinct()
            .sorted()

        if (names.isEmpty()) {
            Toast.makeText(ctx, "Ana sayfası olan başka eklenti bulunamadı", Toast.LENGTH_LONG).show()
            return
        }

        val disabled = HomeHubPrefs.disabled().toMutableSet()
        val checked = BooleanArray(names.size) { names[it] !in disabled }

        AlertDialog.Builder(ctx)
            .setTitle("HomeHub: ana sayfada gösterilecekler")
            .setMultiChoiceItems(names.toTypedArray(), checked) { _, i, isOn ->
                if (isOn) disabled.remove(names[i]) else disabled.add(names[i])
            }
            .setPositiveButton("Kaydet") { _, _ ->
                HomeHubPrefs.setDisabled(disabled)
                Toast.makeText(
                    ctx,
                    "Kaydedildi. Ana sayfayı yenile veya uygulamayı yeniden aç",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
