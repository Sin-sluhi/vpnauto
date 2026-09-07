package ru.miroslav.vpnauto

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.miroslav.vpnauto.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    data class AppItem(val label: String, val pkg: String, val icon: Drawable)
    sealed class Row { data class Header(val title: String) : Row(); data class App(val item: AppItem) : Row() }

    private lateinit var b: ActivityMainBinding
    private lateinit var rules: MutableMap<String, Rule>
    private var all: List<AppItem> = emptyList()
    private lateinit var adapter: RowAdapter
    private var pendingPkg: String? = null
    private var query = ""

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val pkg = pendingPkg ?: return@registerForActivityResult
        pendingPkg = null
        if (res.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val bundle = res.data?.getBundleExtra(VpnAutoService.TASKER_BUNDLE) ?: return@registerForActivityResult
        val guid = bundle.getString(VpnAutoService.GUID_KEY) ?: return@registerForActivityResult
        val blurb = res.data?.getStringExtra(VpnAutoService.TASKER_BLURB).orEmpty()
        val label = blurb.substringAfterLast(":").trim().ifEmpty { if (guid == VpnAutoService.DEFAULT_GUID) "Текущий" else "v2rayNG" }
        rules[pkg] = Rule(guid, label); Prefs.save(this, rules); rebuild()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        rules = Prefs.load(this).toMutableMap()
        all = installedApps()
        adapter = RowAdapter()
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter

        b.statusCard.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        b.batteryCard.setOnClickListener {
            if (!batteryOk()) startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
        }
        b.search.doAfterTextChanged { query = it?.toString()?.trim()?.lowercase().orEmpty(); rebuild() }

        b.delayMinus.setOnClickListener { setDelay(Prefs.stopDelaySec(this) - 1) }
        b.delayPlus.setOnClickListener { setDelay(Prefs.stopDelaySec(this) + 1) }
        setDelay(Prefs.stopDelaySec(this))
        rebuild()
    }

    override fun onResume() { super.onResume(); updateStatus(); updateBattery() }

    private fun batteryOk() = (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

    private fun updateBattery() {
        val ok = batteryOk()
        b.batteryChip.text = getString(if (ok) R.string.battery_ok else R.string.battery_fix)
        b.batteryChip.setBackgroundResource(if (ok) R.drawable.chip_on else R.drawable.chip_off)
        b.batteryChip.setTextColor(color(if (ok) R.color.bg else R.color.text))
    }

    private fun setDelay(sec: Int) {
        Prefs.setStopDelaySec(this, sec)
        b.delayValue.text = getString(R.string.delay_value, Prefs.stopDelaySec(this))
    }

    private fun updateStatus() {
        val on = isServiceEnabled()
        b.statusTitle.text = getString(if (on) R.string.status_on else R.string.status_off)
        b.statusHint.text = getString(if (on) R.string.status_on_hint else R.string.status_off_hint)
        b.statusDot.backgroundTintList = ColorStateList.valueOf(color(if (on) R.color.teal else R.color.danger))
        b.statusRing.backgroundTintList = ColorStateList.valueOf(color(if (on) R.color.teal else R.color.danger))
    }

    private fun rebuild() {
        val shown = if (query.isEmpty()) all else all.filter { it.label.lowercase().contains(query) || it.pkg.contains(query) }
        val with = shown.filter { rules.containsKey(it.pkg) }
        val without = shown.filter { !rules.containsKey(it.pkg) }
        val rows = mutableListOf<Row>()
        if (with.isNotEmpty()) { rows += Row.Header(getString(R.string.section_with)); with.forEach { rows += Row.App(it) } }
        else if (query.isEmpty()) rows += Row.Header(getString(R.string.empty_rules))
        if (without.isNotEmpty()) { rows += Row.Header(getString(R.string.section_without)); without.forEach { rows += Row.App(it) } }
        adapter.submit(rows)
        b.count.text = if (rules.isEmpty()) "" else "${rules.size}"
    }

    private fun pick(pkg: String) {
        pendingPkg = pkg
        val intent = Intent(VpnAutoService.TASKER_EDIT).setPackage(VpnAutoService.V2RAYNG_PKG)
        // Фильтр TaskerActivity в v2rayNG без CATEGORY_DEFAULT — неявный запуск не сработает, ищем компонент вручную.
        val target = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL).firstOrNull()?.activityInfo
        if (target == null) { pendingPkg = null; Toast.makeText(this, R.string.no_v2rayng, Toast.LENGTH_LONG).show(); return }
        intent.setClassName(target.packageName, target.name)
        rules[pkg]?.let { r ->
            intent.putExtra(VpnAutoService.TASKER_BUNDLE, Bundle().apply {
                putBoolean(VpnAutoService.SWITCH_KEY, true); putString(VpnAutoService.GUID_KEY, r.guid)
            })
        }
        try {
            Toast.makeText(this, R.string.pick_hint, Toast.LENGTH_SHORT).show()
            picker.launch(intent)
        } catch (e: ActivityNotFoundException) {
            pendingPkg = null
            Toast.makeText(this, R.string.no_v2rayng, Toast.LENGTH_LONG).show()
        }
    }

    private fun onChipTap(item: AppItem) {
        val r = rules[item.pkg]
        if (r == null) { pick(item.pkg); return }
        MaterialAlertDialogBuilder(this)
            .setTitle(item.label)
            .setMessage(r.label)
            .setPositiveButton(R.string.rule_change) { _, _ -> pick(item.pkg) }
            .setNegativeButton(R.string.rule_remove) { _, _ -> rules.remove(item.pkg); Prefs.save(this, rules); rebuild() }
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        if (VpnAutoService.running) return true
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val me = "$packageName/${VpnAutoService::class.java.name}"
        return am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { it.id == me }
    }

    private fun installedApps(): List<AppItem> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    inner class RowAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var rows: List<Row> = emptyList()
        fun submit(r: List<Row>) { rows = r; notifyDataSetChanged() }
        override fun getItemCount() = rows.size
        override fun getItemViewType(pos: Int) = if (rows[pos] is Row.Header) 0 else 1

        inner class HVH(v: View) : RecyclerView.ViewHolder(v) { val t: TextView = v.findViewById(R.id.header) }
        inner class AVH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.icon)
            val label: TextView = v.findViewById(R.id.label)
            val pkg: TextView = v.findViewById(R.id.pkg)
            val chip: TextView = v.findViewById(R.id.chip)
        }

        override fun onCreateViewHolder(p: ViewGroup, type: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(p.context)
            return if (type == 0) HVH(inf.inflate(R.layout.item_header, p, false)) else AVH(inf.inflate(R.layout.item_app, p, false))
        }

        override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
            when (val row = rows[pos]) {
                is Row.Header -> (h as HVH).t.text = row.title
                is Row.App -> (h as AVH).apply {
                    val item = row.item
                    icon.setImageDrawable(item.icon); label.text = item.label; pkg.text = item.pkg
                    val r = rules[item.pkg]
                    chip.text = r?.label ?: getString(R.string.add)
                    chip.setBackgroundResource(if (r != null) R.drawable.chip_on else R.drawable.chip_off)
                    chip.setTextColor(color(if (r != null) R.color.bg else R.color.text))
                    chip.setOnClickListener { onChipTap(item) }
                    itemView.setOnClickListener { onChipTap(item) }
                }
            }
        }
    }
}
