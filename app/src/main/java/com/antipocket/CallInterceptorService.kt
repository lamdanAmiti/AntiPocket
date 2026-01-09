package com.antipocket

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.view.accessibility.AccessibilityEvent

class CallInterceptorService : AccessibilityService() {

    private lateinit var prefs: PreferencesManager
    private var lastInterceptTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = PreferencesManager.getInstance(this)

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        instance = this
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefs.secureCallsEnabled) return

        val packageName = event.packageName?.toString() ?: return

        // Detect dialer/phone apps and calling UI
        val isDialerPackage = packageName.contains("dialer") ||
                packageName.contains("phone") ||
                packageName.contains("incallui") ||
                packageName == "com.android.server.telecom" ||
                packageName == "com.google.android.dialer" ||
                packageName == "com.samsung.android.incallui" ||
                packageName == "com.android.incallui"

        if (!isDialerPackage) return

        // Check if this looks like an outgoing call screen
        val rootNode = rootInActiveWindow ?: return

        try {
            val text = rootNode.toString().lowercase()
            val isOutgoingCall = text.contains("dialing") ||
                    text.contains("calling") ||
                    text.contains("outgoing") ||
                    (event.className?.toString()?.lowercase()?.contains("incall") == true)

            if (isOutgoingCall || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // Debounce - don't intercept more than once per 3 seconds
                val now = System.currentTimeMillis()
                if (now - lastInterceptTime < 3000) return

                // Check if we're already showing the slider or just completed it
                if (prefs.pendingCallNumber != null) return

                // Check pocket detection if enabled
                if (prefs.onlyWhenInPocket && !PocketDetectionService.isInPocket) {
                    return
                }

                lastInterceptTime = now

                // End the current call
                endCall()

                // We need to get the number somehow - for now just show slider
                // The user will need to redial after confirming
                showSlider()
            }
        } finally {
            rootNode.recycle()
        }
    }

    private fun endCall() {
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telecomManager.endCall()
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun showSlider() {
        val intent = Intent(this, SliderActivity::class.java).apply {
            putExtra(SliderActivity.EXTRA_MODE, SliderActivity.MODE_CALL)
            putExtra(SliderActivity.EXTRA_REDIAL, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    fun lockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    override fun onInterrupt() {
        isRunning = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        instance = null
    }

    companion object {
        var isRunning = false
            private set

        private var instance: CallInterceptorService? = null

        fun lockScreenViaAccessibility(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && instance != null) {
                instance?.lockScreen()
                true
            } else {
                false
            }
        }
    }
}
