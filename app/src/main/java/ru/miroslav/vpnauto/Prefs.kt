package ru.miroslav.vpnauto

import android.content.Context
import org.json.JSONObject

/** Правило для приложения: какой конфиг v2rayNG поднимать. guid "Default" = текущий выбранный в v2rayNG. */
data class Rule(val guid: String, val label: String)

object Prefs {
    private const val FILE = "vpnauto"
    private const val KEY_RULES = "rules_v2"
    private const val KEY_DELAY = "stop_delay_sec"

    fun load(ctx: Context): Map<String, Rule> {
        val raw = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_RULES, null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { k ->
                val r = o.getJSONObject(k); Rule(r.getString("guid"), r.optString("label", "v2rayNG"))
            }
        }.getOrDefault(emptyMap())
    }

    fun save(ctx: Context, rules: Map<String, Rule>) {
        val o = JSONObject()
        rules.forEach { (k, r) -> o.put(k, JSONObject().put("guid", r.guid).put("label", r.label)) }
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_RULES, o.toString()).apply()
    }

    fun stopDelaySec(ctx: Context): Int =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_DELAY, 3)

    fun setStopDelaySec(ctx: Context, sec: Int) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY_DELAY, sec.coerceIn(1, 30)).apply()
}
