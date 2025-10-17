package com.kalasri.safealert.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MySmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("MySmsReceiver", "SMS received or sent event triggered")
        // You can add logic here if needed
    }
}
