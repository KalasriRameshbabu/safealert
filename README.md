# 🚨 SafeAlert – Smart Personal Safety App

SafeAlert is a **personal safety application** designed to send instant alerts during emergency situations. The app triggers a loud siren, sends live location updates to pre-saved emergency contacts, and automatically records a short video during an alert — ensuring quick help and reliable evidence when it matters most.



## 💡 Features

* 🔔 **Emergency Siren** – Plays a loud siren sound when the alert is triggered.
* 📍 **Live Location Sharing** – Sends your real-time GPS location via SMS to emergency contacts.
* 🎥 **Automatic Video Recording** – Records a short video during an alert and uploads it to Firebase for evidence.
* 📱 **Multiple Alert Modes**

  * **Start Alert Button** – Manual alert trigger.
  * **Shake Detection** – Automatically activates alert when the phone is shaken three times.
* ☁️ **Firebase Integration** – Stores alert videos securely in Firebase Storage.



## 🧩 Tech Stack

| Component           | Technology Used                      |
| ------------------- | ------------------------------------ |
| **Frontend (App)**  | Android Studio (Java/Kotlin)         |
| **Backend**         | Firebase Realtime Database & Storage |
| **APIs Used**       | Google Maps API (for live location)
                        Firebase Authentication API
                        Firebase Storage API
                        SMS Manager API
                        CameraX / MediaRecorder API (for video recording)
                        LocationManager API (for GPS tracking)        |
| **Alert System**    | SMS Manager, MediaPlayer (for Siren) |



## ⚙️ How It Works

1. User adds emergency contact numbers in the app.
2. When the **alert** is triggered (via button or shake), the app:

   * Plays a **siren sound** 🔊
   * Fetches the **current GPS location** 📍
   * Sends an **SMS with location link** to all saved contacts 📩
   * **Records a short video** and send in whatsapp🎥
3. The alert data and uploaded video can later be accessed for verification or evidence.



## 📷 Project Architecture


SafeAlert/
│
├── app/
│   ├── java/
│   │   └── com.safealert/
│   │       ├── MainActivity.java
│   │       ├── ShakeDetector.java
│   │       ├── LocationService.java
│   │       └── FirebaseUploader.java
│   ├── res/
│   │   ├── layout/
│   │   └── drawable/
│   └── manifest/
│
├── hardware/
│   ├── nodemcu_pir_sensor_code.ino
│   └── connection_diagram.png
│
└── README.md
```



## 🧠 Future Improvements

* 🎙️ Add voice recording during alert.
* 🔐 Secure dashboard for contact management.
* 🌐 Cloud-based notification system (email + push notifications).
* ⚡ Faster video upload compression for poor networks.



## 🧑‍💻 Developed By

**Kala Sri**
Cybersecurity Engineering Student | Android Developer | IoT Enthusiast


## 📄 License

This project is open-source and available under the [MIT License]



.


