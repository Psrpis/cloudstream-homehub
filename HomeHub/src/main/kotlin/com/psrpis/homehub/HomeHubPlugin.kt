package com.psrpis.homehub

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
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
        val items = orderedSections().toMutableList()
        if (items.isEmpty()) {
            Toast.makeText(ctx, "Ana sayfası olan başka eklenti bulunamadı", Toast.LENGTH_LONG).show()
            return
        }

        val off = HomeHubPrefs.disabledSections()
        val enabled = items.filter { it.key !in off }.map { it.key }.toMutableSet()

        val density = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val scroll = ScrollView(ctx).apply { addView(col) }

        fun render() {
            col.removeAllViews()

            // Üst satır: hepsini aç / kapat
            val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            top.addView(Button(ctx).apply {
                text = "Tümünü aç"
                setOnClickListener { enabled.addAll(items.map { it.key }); render() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            top.addView(Button(ctx).apply {
                text = "Tümünü kapat"
                setOnClickListener { enabled.clear(); render() }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            col.addView(top)

            items.forEachIndexed { i, s ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(CheckBox(ctx).apply {
                    text = s.title
                    isChecked = s.key in enabled
                    setOnCheckedChangeListener { _, on ->
                        if (on) enabled.add(s.key) else enabled.remove(s.key)
                    }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

                row.addView(Button(ctx).apply {
                    text = "▲"
                    minWidth = 0
                    minimumWidth = 0
                    isEnabled = i > 0
                    setOnClickListener {
                        val t = items[i]; items[i] = items[i - 1]; items[i - 1] = t
                        render()
                    }
                }, LinearLayout.LayoutParams(dp(48), dp(44)))

                row.addView(Button(ctx).apply {
                    text = "▼"
                    minWidth = 0
                    minimumWidth = 0
                    isEnabled = i < items.size - 1
                    setOnClickListener {
                        val t = items[i]; items[i] = items[i + 1]; items[i + 1] = t
                        render()
                    }
                }, LinearLayout.LayoutParams(dp(48), dp(44)))

                col.addView(row)
            }
        }
        render()

        AlertDialog.Builder(ctx)
            .setTitle("HomeHub: bölümler ve sıra")
            .setView(scroll)
            .setPositiveButton("Kaydet") { _, _ ->
                HomeHubPrefs.setOrder(items.map { it.key })
                HomeHubPrefs.setDisabledSections(
                    items.map { it.key }.filter { it !in enabled }.toSet()
                )
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
