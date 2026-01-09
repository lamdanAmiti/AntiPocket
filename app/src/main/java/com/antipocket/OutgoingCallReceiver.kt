package com.antipocket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OutgoingCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_NEW_OUTGOING_CALL) return

        val prefs = PreferencesManager.getInstance(context)

        // Check if secure calls feature is enabled
        if (!prefs.secureCallsEnabled) return

        val phoneNumber = resultData ?: intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER) ?: return

        // Skip emergency numbers
        if (isEmergencyNumber(phoneNumber)) return

        // Check if "only when in pocket" is enabled
        if (prefs.onlyWhenInPocket) {
            // Check if currently in pocket
            if (!PocketDetectionService.isInPocket) {
                // Not in pocket, allow call to proceed normally
                return
            }
        }

        // Intercept the call
        prefs.pendingCallNumber = phoneNumber

        // Cancel the current call
        resultData = null

        // Show the slider activity
        SliderActivity.startForCall(context)
    }

    private fun isEmergencyNumber(number: String): Boolean {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        val emergencyNumbers = listOf("911", "112", "999", "000", "110", "119", "100", "101", "102", "108")
        return emergencyNumbers.any { cleaned == it || cleaned.endsWith(it) && cleaned.length <= 4 }
    }
}
