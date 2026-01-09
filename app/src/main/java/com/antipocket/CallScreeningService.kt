package com.antipocket

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse

class AntiPocketCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val prefs = PreferencesManager.getInstance(this)

        // Only handle outgoing calls
        if (callDetails.callDirection != Call.Details.DIRECTION_OUTGOING) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Check if secure calls feature is enabled
        if (!prefs.secureCallsEnabled) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Get phone number
        val phoneNumber = callDetails.handle?.schemeSpecificPart ?: ""

        // Skip emergency numbers
        if (isEmergencyNumber(phoneNumber)) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Check if "only when in pocket" is enabled
        if (prefs.onlyWhenInPocket && !PocketDetectionService.isInPocket) {
            // Not in pocket, allow call to proceed normally
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Block the call and show slider
        prefs.pendingCallNumber = phoneNumber

        // Reject/disallow the call
        val response = CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipNotification(true)
            .build()

        respondToCall(callDetails, response)

        // Show the slider activity
        SliderActivity.startForCall(this)
    }

    private fun isEmergencyNumber(number: String): Boolean {
        val cleaned = number.replace(Regex("[^0-9]"), "")
        val emergencyNumbers = listOf("911", "112", "999", "000", "110", "119", "100", "101", "102", "108")
        return emergencyNumbers.any { cleaned == it || (cleaned.endsWith(it) && cleaned.length <= 4) }
    }
}
