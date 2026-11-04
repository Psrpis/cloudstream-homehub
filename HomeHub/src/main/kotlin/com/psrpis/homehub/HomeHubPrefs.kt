package com.psrpis.homehub

import android.content.Context
import android.content.SharedPreferences

/** Bölüm sırası ve kapatılan bölümler burada saklanır. */
object HomeHubPrefs {
    private const val FILE = "homehub_prefs"
    private const val KEY_ORDER = "order"
    private const val KEY_OFF = "disabled_sections"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    /** Kaydedilmiş bölüm anahtarları, sırasıyla. */
    fun order(): List<String> =
        prefs?.getString(KEY_ORDER, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    fun setOrder(keys: List<String>) {
        prefs?.edit()?.putString(KEY_ORDER, keys.joinToString("\n"))?.apply()
    }

    /** Kapatılan bölümlerin anahtarları. Boşsa hepsi açık. */
    fun disabledSections(): Set<String> =
        prefs?.getStringSet(KEY_OFF, emptySet())?.toSet() ?: emptySet()

    fun setDisabledSections(keys: Set<String>) {
        prefs?.edit()?.putStringSet(KEY_OFF, keys)?.apply()
    }
}
