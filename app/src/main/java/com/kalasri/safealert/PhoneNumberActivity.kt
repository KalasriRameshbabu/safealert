package com.kalasri.safealert

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class PhoneNumberActivity : AppCompatActivity() {
    private lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number)

        pref = getSharedPreferences("SafeAlertPrefs", MODE_PRIVATE)

        val phone1 = findViewById<EditText>(R.id.phoneNumber1)
        val phone2 = findViewById<EditText>(R.id.phoneNumber2)
        val phone3 = findViewById<EditText>(R.id.phoneNumber3)
        val saveBtn = findViewById<Button>(R.id.saveNumbers)

        saveBtn.setOnClickListener {
            pref.edit()
                .putString("phone1", phone1.text.toString())
                .putString("phone2", phone2.text.toString())
                .putString("phone3", phone3.text.toString())
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}












