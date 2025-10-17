package com.kalasri.safealert

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (resultCode) {
            Activity.RESULT_OK ->
                Toast.makeText(context, "✅ SMS sent successfully!", Toast.LENGTH_SHORT).show()
            SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                Toast.makeText(context, "❌ SMS failed: Generic failure", Toast.LENGTH_SHORT).show()
            SmsManager.RESULT_ERROR_NO_SERVICE ->
                Toast.makeText(context, "❌ SMS failed: No service", Toast.LENGTH_SHORT).show()
            SmsManager.RESULT_ERROR_NULL_PDU ->
                Toast.makeText(context, "❌ SMS failed: Null PDU", Toast.LENGTH_SHORT).show()
            SmsManager.RESULT_ERROR_RADIO_OFF ->
                Toast.makeText(context, "❌ SMS failed: Radio off", Toast.LENGTH_SHORT).show()
        }
    }
}


