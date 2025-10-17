package com.kalasri.safealert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.widget.Toast

class CallStateReceiver : BroadcastReceiver() {

    // Use a static variable to track the previous state
    companion object {
        private var wasOffHook = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Ensure the intent action is for phone state changes
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val prefs = context.getSharedPreferences("SafeAlertPrefs", Context.MODE_PRIVATE)
        val isEmergencyCall = prefs.getBoolean("isEmergencyCallInProgress", false)

        if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
            wasOffHook = true
        }

        // Check if the state just changed from OFFHOOK to IDLE
        if (state == TelephonyManager.EXTRA_STATE_IDLE && wasOffHook) {
            wasOffHook = false // Reset the state tracker

            if (isEmergencyCall) {
                // It was our emergency call that just ended
                Toast.makeText(context, "Emergency call ended. Starting video...", Toast.LENGTH_SHORT).show()

                // 1. Reset the flag so this doesn't trigger again for normal calls
                prefs.edit().putBoolean("isEmergencyCallInProgress", false).apply()

                // 2. Launch MainActivity with a special command to start video
                val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("ACTION_START_VIDEO", true)
                }
                context.startActivity(mainActivityIntent)
            }
        }
    }
}