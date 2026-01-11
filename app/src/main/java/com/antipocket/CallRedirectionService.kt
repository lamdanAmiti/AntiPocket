package com.antipocket

import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class AntiPocketCallRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        // Check if this call should bypass interception (user already confirmed)
        if (shouldBypass) {
            shouldBypass = false
            placeCallUnmodified()
            return
        }

        val prefs = PreferencesManager.getInstance(this)

        // Check if secure calls feature is enabled
        if (!prefs.secureCallsEnabled) {
            placeCallUnmodified()
            return
        }

        // Get phone number
        val phoneNumber = handle.schemeSpecificPart ?: ""

        // Skip emergency numbers
        if (isEmergencyNumber(phoneNumber)) {
            placeCallUnmodified()
            return
        }

        // Check if "only when in pocket" is enabled
        if (prefs.onlyWhenInPocket && !PocketDetectionService.isInPocket) {
            placeCallUnmodified()
            return
        }

        // Store the number and cancel the call
        prefs.pendingCallNumber = phoneNumber

        // Mark that we just intercepted a call (to prevent AccessibilityService from also intercepting)
        lastInterceptTime = System.currentTimeMillis()

        // Cancel the call
        cancelCall()

        // Show the slider activity
        SliderActivity.startForCall(this)
    }

    private fun isEmergencyNumber(number: String): Boolean {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        val emergencyNumbers = listOf("911", "112", "999", "000", "110", "119", "100", "101", "102", "108")
        return emergencyNumbers.any { cleaned == it || (cleaned.endsWith(it) && cleaned.length <= 4) }
    }

    companion object {
        @Volatile
        var shouldBypass = false

        @Volatile
        var lastInterceptTime = 0L
    }
}
