package ru.miroslav.vpnauto

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast

class VpnAutoService : AccessibilityService() {

    companion object {
        private const val TAG = "VpnAuto"
        @Volatile var running = false

        const val V2RAYNG_PKG = "com.v2ray.ang"
        const val TASKER_EDIT = "com.twofortyfouram.locale.intent.action.EDIT_SETTING"
        const val TASKER_FIRE = "com.twofortyfouram.locale.intent.action.FIRE_SETTING"
        const val TASKER_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
        const val TASKER_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"
        const val SWITCH_KEY = "tasker_extra_bundle_switch"
        const val GUID_KEY = "tasker_extra_bundle_guid"
        const val DEFAULT_GUID = "Default"

        /** Не считаются "выходом" из приложения: шторка, оверлеи, сам VPN-клиент. */
        private val IGNORE = setOf(
            "com.android.systemui",
            "com.samsung.android.app.cocktailbarservice",
            "com.samsung.android.biometrics.app.setting",
            V2RAYNG_PKG, "ru.miroslav.vpnauto",
        )
        /** Пауза между stop и start при смене конфига — v2rayNG нужно время погасить туннель. */
        private const val RESTART_GAP_MS = 900L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentGuid: String? = null
    private val stopRunnable = Runnable { stopVpn() }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in IGNORE || isIme(pkg)) return

        val rule = Prefs.load(this)[pkg]
        handler.removeCallbacks(stopRunnable)
        when {
            rule == null && currentGuid != null ->
                handler.postDelayed(stopRunnable, Prefs.stopDelaySec(this) * 1000L)
            rule != null && rule.guid != currentGuid -> startVpn(rule)
        }
    }

    private fun startVpn(rule: Rule) {
        val switching = currentGuid != null
        currentGuid = rule.guid
        Toast.makeText(this, "VpnAuto → ${rule.label}", Toast.LENGTH_SHORT).show()
        if (switching) {
            fire(false, DEFAULT_GUID)
            handler.postDelayed({ fire(true, rule.guid) }, RESTART_GAP_MS)
        } else fire(true, rule.guid)
    }

    private fun stopVpn() {
        currentGuid = null
        Toast.makeText(this, "VpnAuto: VPN выключен", Toast.LENGTH_SHORT).show()
        fire(false, DEFAULT_GUID)
    }

    private fun fire(on: Boolean, guid: String) {
        Log.i(TAG, "fire on=$on guid=$guid")
        val bundle = Bundle().apply { putBoolean(SWITCH_KEY, on); putString(GUID_KEY, guid) }
        sendBroadcast(Intent(TASKER_FIRE).apply { setPackage(V2RAYNG_PKG); putExtra(TASKER_BUNDLE, bundle) })
    }

    private fun isIme(pkg: String): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == pkg }
    }

    override fun onServiceConnected() {
        super.onServiceConnected(); running = true
        Toast.makeText(this, "VpnAuto: служба запущена", Toast.LENGTH_SHORT).show()
    }
    override fun onUnbind(intent: Intent?): Boolean { running = false; return super.onUnbind(intent) }
    override fun onInterrupt() {}
    override fun onDestroy() { running = false; handler.removeCallbacksAndMessages(null); super.onDestroy() }
}
