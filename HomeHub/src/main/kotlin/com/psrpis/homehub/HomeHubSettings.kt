package com.psrpis.homehub

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Ultima'daki gibi: Yapılandır, Sırala, Sıfırla ekranları. Tamamı kodla çizilir (XML yok). */
internal object HomeHubSettings {

    private fun dp(ctx: Context, v: Int): Int =
        (v * ctx.resources.displayMetrics.density).toInt()

    private fun themeColor(ctx: Context, attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        if (!ctx.theme.resolveAttribute(attr, tv, true)) return fallback
        return if (tv.resourceId != 0) ctx.getColor(tv.resourceId) else tv.data
    }

    private fun label(
        ctx: Context,
        s: String,
        sp: Float,
        bold: Boolean = false,
        secondary: Boolean = false
    ): TextView {
        val tv = TextView(ctx)
        tv.text = s
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        tv.setTextColor(
            if (secondary) themeColor(ctx, android.R.attr.textColorSecondary, Color.GRAY)
            else themeColor(ctx, android.R.attr.textColorPrimary, Color.WHITE)
        )
        return tv
    }

    private fun vertical(ctx: Context): LinearLayout {
        val l = LinearLayout(ctx)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8))
        return l
    }

    private fun toast(ctx: Context, msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
    }

    private fun header(ctx: Context, s: String): TextView {
        val tv = label(ctx, s, 13f, bold = true, secondary = true)
        tv.setPadding(0, dp(ctx, 16), 0, dp(ctx, 4))
        return tv
    }

    private fun row(ctx: Context, title: String, sub: String, onClick: () -> Unit): View {
        val l = LinearLayout(ctx)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(0, dp(ctx, 10), 0, dp(ctx, 10))
        l.addView(label(ctx, title, 16f, bold = true))
        l.addView(label(ctx, sub, 13f, secondary = true))
        l.isClickable = true
        l.setOnClickListener { onClick() }
        return l
    }

    // ---------------------------------------------------------------- Ana ekran

    fun show(ctx: Context) {
        val root = vertical(ctx)
        val dialog = AlertDialog.Builder(ctx)
            .setTitle("HomeHub Ayarları")
            .setView(ScrollView(ctx).also { it.addView(root) })
            .setNegativeButton("Kapat", null)
            .create()

        root.addView(header(ctx, "BÖLÜMLER VE EKLENTİLER"))
        root.addView(
            row(
                ctx, "Yapılandır",
                "Hangi eklentilerin hangi bölümleri ana sayfada görünsün"
            ) {
                dialog.dismiss()
                showConfigure(ctx)
            }
        )
        root.addView(
            row(ctx, "Sırala", "Ana sayfa bölümlerinin sırasını değiştir") {
                dialog.dismiss()
                showReorder(ctx)
            }
        )

        root.addView(header(ctx, "TEHLİKELİ BÖLGE"))
        root.addView(
            row(ctx, "Sıfırla", "Seçimleri sil, varsayılana (tüm bölümler) dön") {
                dialog.dismiss()
                AlertDialog.Builder(ctx)
                    .setTitle("Sıfırla")
                    .setMessage("Seçtiğin tüm bölümler ve sıralama silinecek. Emin misin?")
                    .setPositiveButton("Sıfırla") { _, _ ->
                        HomeHubPrefs.reset()
                        toast(ctx, "Sıfırlandı. Ana sayfayı yenile veya uygulamayı yeniden aç")
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }
        )
        dialog.show()
    }

    // ---------------------------------------------------------------- Yapılandır

    private fun showConfigure(ctx: Context) {
        val live = liveSections()
        if (live.isEmpty()) {
            toast(ctx, "Ana sayfası olan başka eklenti bulunamadı")
            return
        }

        // Taslak: anahtar -> öncelik. Daha önce kaydedilmediyse hepsi açık başlar.
        val draft = LinkedHashMap<String, Int>()
        if (HomeHubPrefs.configured) draft.putAll(HomeHubPrefs.sections())
        else live.forEach { s -> draft[s.key] = 0 }
        var showName = HomeHubPrefs.showExtensionName

        val root = vertical(ctx)
        root.addView(
            label(
                ctx,
                "Öncelik sayısı büyük olan bölüm ana sayfada üstte durur. " +
                    "Sırala ekranı bu sayıları sizin yerinize günceller.",
                12f, secondary = true
            )
        )

        val sw = Switch(ctx)
        sw.text = "Ana sayfada eklenti adını göster"
        sw.isChecked = showName
        sw.setOnCheckedChangeListener { _, c -> showName = c }
        sw.setPadding(0, dp(ctx, 12), 0, dp(ctx, 8))
        root.addView(sw)

        live.groupBy { s -> s.provider }.forEach { (provider, sections) ->
            val head = label(ctx, "", 16f, bold = true)
            head.setPadding(0, dp(ctx, 12), 0, dp(ctx, 6))
            val body = vertical(ctx)
            body.setPadding(dp(ctx, 8), 0, 0, 0)
            body.visibility = View.GONE

            fun refreshHead() {
                val n = sections.count { s -> draft.containsKey(s.key) }
                val arrow = if (body.visibility == View.VISIBLE) "▾" else "▸"
                head.text = "$arrow $provider  ($n/${sections.size})"
            }

            sections.forEach { s ->
                val line = LinearLayout(ctx)
                line.orientation = LinearLayout.HORIZONTAL
                line.gravity = Gravity.CENTER_VERTICAL

                val cb = CheckBox(ctx)
                cb.text = s.name.ifBlank { "(ana bölüm)" }
                cb.isChecked = draft.containsKey(s.key)
                cb.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                val et = EditText(ctx)
                et.setText((draft[s.key] ?: 0).toString())
                et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
                et.setEms(3)
                et.isEnabled = cb.isChecked
                et.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                    override fun afterTextChanged(p0: Editable?) {
                        if (draft.containsKey(s.key)) {
                            draft[s.key] = p0?.toString()?.toIntOrNull() ?: 0
                        }
                    }
                })

                cb.setOnCheckedChangeListener { _, checked ->
                    if (checked) draft[s.key] = et.text.toString().toIntOrNull() ?: 0
                    else draft.remove(s.key)
                    et.isEnabled = checked
                    refreshHead()
                }

                line.addView(cb)
                line.addView(et)
                body.addView(line)
            }

            head.setOnClickListener {
                body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                refreshHead()
            }
            refreshHead()
            root.addView(head)
            root.addView(body)
        }

        AlertDialog.Builder(ctx)
            .setTitle("Bölümleri yapılandır")
            .setView(ScrollView(ctx).also { it.addView(root) })
            .setPositiveButton("Kaydet") { _, _ ->
                HomeHubPrefs.showExtensionName = showName
                HomeHubPrefs.saveSections(draft)
                toast(ctx, "Kaydedildi. Ana sayfayı yenile veya uygulamayı yeniden aç")
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ---------------------------------------------------------------- Sırala

    private class RowHolder(
        view: View,
        val title: TextView,
        val up: TextView,
        val down: TextView
    ) : RecyclerView.ViewHolder(view)

    private fun showReorder(ctx: Context) {
        val live = liveSections().associateBy { s -> s.key }
        val saved = HomeHubPrefs.sections()
        val keys: MutableList<String> =
            if (HomeHubPrefs.configured) {
                saved.entries.sortedByDescending { e -> e.value }
                    .map { e -> e.key }
                    .filter { k -> live.containsKey(k) }
                    .toMutableList()
            } else {
                live.keys.toMutableList()
            }

        if (keys.isEmpty()) {
            toast(ctx, "Hiç bölüm seçmedin. Önce Yapılandır'dan bölüm seç")
            return
        }

        val rv = RecyclerView(ctx)
        rv.layoutManager = LinearLayoutManager(ctx)
        rv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(ctx, 380)
        )

        val adapter = object : RecyclerView.Adapter<RowHolder>() {
            override fun getItemCount(): Int = keys.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
                val l = LinearLayout(parent.context)
                l.orientation = LinearLayout.HORIZONTAL
                l.gravity = Gravity.CENTER_VERTICAL
                l.setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 8), dp(ctx, 10))
                l.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val t = label(ctx, "", 15f)
                t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                val up = label(ctx, "▲", 20f)
                up.setPadding(dp(ctx, 14), dp(ctx, 6), dp(ctx, 14), dp(ctx, 6))
                val down = label(ctx, "▼", 20f)
                down.setPadding(dp(ctx, 14), dp(ctx, 6), dp(ctx, 14), dp(ctx, 6))
                l.addView(t)
                l.addView(up)
                l.addView(down)
                return RowHolder(l, t, up, down)
            }

            override fun onBindViewHolder(h: RowHolder, position: Int) {
                val k = keys[position]
                h.title.text = "${position + 1}. ${live[k]?.title ?: k}"
                h.up.setOnClickListener {
                    val p = h.bindingAdapterPosition
                    if (p > 0) {
                        java.util.Collections.swap(keys, p, p - 1)
                        notifyItemMoved(p, p - 1)
                        notifyItemRangeChanged(p - 1, 2)
                    }
                }
                h.down.setOnClickListener {
                    val p = h.bindingAdapterPosition
                    if (p >= 0 && p < keys.size - 1) {
                        java.util.Collections.swap(keys, p, p + 1)
                        notifyItemMoved(p, p + 1)
                        notifyItemRangeChanged(p, 2)
                    }
                }
            }
        }
        rv.adapter = adapter

        // Basılı tutup sürükleyerek de sıralanır
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from < 0 || to < 0) return false
                java.util.Collections.swap(keys, from, to)
                adapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                adapter.notifyDataSetChanged()
            }
        }).attachToRecyclerView(rv)

        AlertDialog.Builder(ctx)
            .setTitle("Bölümleri sırala")
            .setView(rv)
            .setPositiveButton("Kaydet") { _, _ ->
                val out = LinkedHashMap<String, Int>()
                // Şu an yüklü olmayan eklentilerin eski seçimleri korunur
                saved.forEach { (k, v) -> if (!live.containsKey(k)) out[k] = v }
                keys.forEachIndexed { i, k -> out[k] = keys.size - i }
                HomeHubPrefs.saveSections(out)
                toast(ctx, "Kaydedildi. Ana sayfayı yenile veya uygulamayı yeniden aç")
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}
