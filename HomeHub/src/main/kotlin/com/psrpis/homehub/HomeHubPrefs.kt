package com.psrpis.homehub

import android.content.Context
import android.content.SharedPreferences

/** Ayarlar sayfasında kapatılan eklentilerin adlarını saklar. Boşsa hepsi açık. */
object HomeHubPrefs {
    private const val FILE = "homehub_prefs"
    private const val KEY_DISABLED = "disabled"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun disabled(): Set<String> =
        prefs?.getStringSet(KEY_DISABLED, emptySet())?.toSet() ?: emptySet()

    fun setDisabled(names: Set<String>) {
        prefs?.edit()?.putStringSet(KEY_DISABLED, names)?.apply()
    }
}
