package com.psrpis.homehub

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Ayarlar: hangi bölümler açık (anahtar -> öncelik), eklenti adı gösterilsin mi.
 * Öncelik büyükse bölüm ana sayfada üstte durur.
 */
object HomeHubPrefs {
    private const val FILE = "homehub_prefs"
    private const val K_CONFIGURED = "configured"
    private const val K_SECTIONS = "sections"
    private const val K_SHOW_NAME = "show_extension_name"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    /** Kullanıcı ayarları en az bir kez kaydetti mi? Hayırsa tüm bölümler gösterilir. */
    var configured: Boolean
        get() = prefs?.getBoolean(K_CONFIGURED, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(K_CONFIGURED, value)?.apply()
        }

    var showExtensionName: Boolean
        get() = prefs?.getBoolean(K_SHOW_NAME, true) ?: true
        set(value) {
            prefs?.edit()?.putBoolean(K_SHOW_NAME, value)?.apply()
        }

    /** anahtar -> öncelik */
    fun sections(): LinkedHashMap<String, Int> {
        val out = LinkedHashMap<String, Int>()
        val raw = prefs?.getString(K_SECTIONS, null) ?: return out
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                out[k] = obj.optInt(k, 0)
            }
        } catch (e: Exception) {
            // bozuk veri: boş say
        }
        return out
    }

    fun saveSections(map: Map<String, Int>) {
        val obj = JSONObject()
        for ((k, v) in map) obj.put(k, v)
        prefs?.edit()
            ?.putString(K_SECTIONS, obj.toString())
            ?.putBoolean(K_CONFIGURED, true)
            ?.apply()
    }

    fun reset() {
        prefs?.edit()?.clear()?.apply()
    }
}
