package com.kalasri.safealert

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.view.KeyEvent
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient // <-- CHANGE: New import
import com.google.android.gms.location.LocationServices // <-- CHANGE: New import
import com.google.android.gms.location.Priority // <-- CHANGE: New import
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {

    // SharedPreferences for storing emergency contacts
    private lateinit var pref: SharedPreferences
    // MediaPlayer for the siren sound
    private lateinit var mediaPlayer: MediaPlayer
    // SensorManager for shake detection
    private lateinit var sensorManager: SensorManager
    private var lastShakeTime: Long = 0

    // --- 📍 LOCATION CHANGES START HERE ---
    // CHANGE: Using the modern FusedLocationProviderClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // CHANGE: Default location message is updated for clarity
    private var currentLocation: String = "Location not yet determined. Trigger an alert to fetch it."
    // --- 📍 LOCATION CHANGES END HERE ---

    // CameraX variables for video capture
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    // Handler for scheduling delayed tasks
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPreferences
        pref = getSharedPreferences("SafeAlertPrefs", MODE_PRIVATE)

        // Request all necessary permissions at once
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ),
            1
        )

        // Initialize MediaPlayer and SensorManager
        mediaPlayer = MediaPlayer.create(this, R.raw.siren)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )

        // CHANGE: Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up button click listeners
        findViewById<Button>(R.id.startAlert).setOnClickListener {
            triggerFullAlert()
        }
        findViewById<Button>(R.id.stealthAlertButton).setOnClickListener {
            triggerStealthAlert()
        }
        findViewById<Button>(R.id.stopAlert).setOnClickListener {
            stopEverything()
        }
    }

    // --- Logic to handle different alert types ---

    private fun triggerFullAlert() {
        Toast.makeText(this, "Full Alert Triggered!", Toast.LENGTH_SHORT).show()
        // Location is now updated on demand when the alert is sent
        updateLocationAndSendAlert("EMERGENCY! I need help.", "full")
    }

    private fun triggerShakeAlert() {
        Toast.makeText(this, "Shake Alert Triggered!", Toast.LENGTH_SHORT).show()
        updateLocationAndSendAlert("EMERGENCY! I need help. (Shake Detected)", "shake")
    }

    private fun triggerStealthAlert() {
        Toast.makeText(this, "Stealth Alert Triggered!", Toast.LENGTH_SHORT).show()
        updateLocationAndSendAlert("Discreet Alert: I may need help.", "stealth")
    }

    // --- Core Action Functions ---

    private fun stopEverything() {
        mainThreadHandler.removeCallbacksAndMessages(null)
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            try {
                mediaPlayer.prepare()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        activeRecording?.stop()
        activeRecording = null
        Toast.makeText(this, "Alert and all actions stopped.", Toast.LENGTH_SHORT).show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            triggerStealthAlert()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun playSiren() {
        if (!mediaPlayer.isPlaying) mediaPlayer.start()
    }

    private fun makeEmergencyCall() {
        val phone1 = pref.getString("phone1", "")
        if (phone1.isNullOrEmpty()) {
            Toast.makeText(this, "Primary emergency number not set.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone1"))
                startActivity(callIntent)
            } catch (e: SecurityException) {
                Toast.makeText(this, "Call failed: Security exception.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Cannot make call. Phone permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendInitialLocationSms(prefix: String) {
        val message = "$prefix My location: $currentLocation. A video may be recorded."
        sendSmsToAllContacts(message)
    }

    private fun startDelayedVideoRecording() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val qualitySelector = QualitySelector.from(Quality.SD)
            val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, videoCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to bind camera.", Toast.LENGTH_SHORT).show()
                return@addListener
            }

            val videoFile = File(getExternalFilesDir(null), "EVIDENCE_VIDEO_${System.currentTimeMillis()}.mp4")
            val outputOptions = FileOutputOptions.Builder(videoFile).build()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio recording permission denied.", Toast.LENGTH_SHORT).show()
                return@addListener
            }

            Toast.makeText(this, "Recording 30-second video...", Toast.LENGTH_LONG).show()
            activeRecording = videoCapture?.output?.prepareRecording(this, outputOptions)
                ?.withAudioEnabled()
                ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                    if (recordEvent is VideoRecordEvent.Finalize && !recordEvent.hasError()) {
                        val videoUri = FileProvider.getUriForFile(this, "${packageName}.provider", videoFile)
                        shareVideoOnWhatsApp(videoUri)
                    } else if (recordEvent is VideoRecordEvent.Finalize && recordEvent.hasError()) {
                        Toast.makeText(this, "Video recording failed.", Toast.LENGTH_SHORT).show()
                    }
                }

            mainThreadHandler.postDelayed({
                activeRecording?.stop()
                activeRecording = null
            }, 30000)

        }, ContextCompat.getMainExecutor(this))
    }

    private fun shareVideoOnWhatsApp(videoUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_STREAM, videoUri)
            putExtra(Intent.EXTRA_TEXT, "Emergency Evidence: Video captured during alert. My location: $currentLocation")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            val chooser = Intent.createChooser(shareIntent, "Share video via...")
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed or failed to open.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSmsToAllContacts(message: String) {
        val phone1 = pref.getString("phone1", "")
        val phone2 = pref.getString("phone2", "")
        val phone3 = pref.getString("phone3", "")
        val contacts = listOf(phone1, phone2, phone3).filterNot { it.isNullOrEmpty() }

        if(contacts.isEmpty()){
            Toast.makeText(this, "No emergency contacts set for SMS.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val smsManager = this.getSystemService(SmsManager::class.java)
            contacts.forEach { phone ->
                smsManager.sendTextMessage(phone, null, message, null, null)
            }
            Toast.makeText(this, "SMS sent to emergency contacts.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "SMS failed to send.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * CHANGE: This new function fetches the location first and then triggers the appropriate actions.
     * This ensures you always send the FRESHEST possible location data.
     */
    private fun updateLocationAndSendAlert(smsPrefix: String, alertType: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            currentLocation = "Location permission not granted."
            // Proceed with the alert anyway, but with a warning message.
            Toast.makeText(this, "Cannot get location. Permission denied.", Toast.LENGTH_LONG).show()
            executeAlertActions(smsPrefix, alertType) // Still send alert, but without accurate location
            return
        }

        // Actively request a fresh location. PRIORITY_HIGH_ACCURACY is best for emergencies.
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Location found! Update the currentLocation string with a CORRECT Google Maps URL.
                    currentLocation = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                } else {
                    currentLocation = "Could not get current location. Please ensure location is ON."
                }
                // Once location is updated (or failed), execute the rest of the alert.
                executeAlertActions(smsPrefix, alertType)
            }
            .addOnFailureListener {
                currentLocation = "Failed to get location."
                // Even if location fails, the alert must go on.
                executeAlertActions(smsPrefix, alertType)
            }
    }

    /**
     * CHANGE: This helper function contains the logic that runs AFTER location is fetched.
     */
    private fun executeAlertActions(smsPrefix: String, alertType: String) {
        // Step 1: Send SMS immediately with the best location info we have.
        sendInitialLocationSms(smsPrefix)

        // Step 2: Perform actions based on the alert type.
        when (alertType) {
            "full" -> {
                playSiren()
                makeEmergencyCall()
                mainThreadHandler.postDelayed({ startDelayedVideoRecording() }, 60000)
            }
            "shake" -> {
                playSiren()
                makeEmergencyCall()
                // No video for shake alert
            }
            "stealth" -> {
                // No siren or call for stealth alert
                mainThreadHandler.postDelayed({ startDelayedVideoRecording() }, 60000)
            }
        }
    }

    // --- Sensor and Lifecycle Methods ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            if (gForce > 2.7 && (System.currentTimeMillis() - lastShakeTime > 10000)) {
                lastShakeTime = System.currentTimeMillis()
                triggerShakeAlert()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used, but required
    }

    override fun onDestroy() {
        super.onDestroy()
        mainThreadHandler.removeCallbacksAndMessages(null)
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }
}





























































































