package com.antipocket

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class CallInterceptorService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            // Minimal event types - we only need this service for lock screen functionality
            eventTypes = 0
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
        instance = this
        isRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op: Call interception is handled by CallRedirectionService
        // This service is only used for lock screen functionality
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
