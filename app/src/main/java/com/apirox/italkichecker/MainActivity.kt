package com.apirox.italkichecker

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var serviceSwitch: SwitchCompat
    private lateinit var lastCheckTime: TextView
    private lateinit var lastCheckSuccess: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        serviceSwitch = findViewById(R.id.serviceSwitch)
        lastCheckTime = findViewById(R.id.tvLastCheckTime)
        lastCheckSuccess = findViewById(R.id.tvSuccessValue)

        serviceSwitch.isChecked = isServiceRunning(CheckAvailabilityService::class.java)
        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val intent = Intent(this, CheckAvailabilityService::class.java)
                ContextCompat.startForegroundService(this, intent)
            } else {
                val intent = Intent(this, CheckAvailabilityService::class.java)
                intent.action = STOP_SERVICE
                ContextCompat.startForegroundService(this, intent)
            }
        }

        val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        lastCheckTime.text = preferences.getString(LAST_CHECK_TIME_KEY, "")
        lastCheckSuccess.text = preferences.getBoolean(LAST_CHECK_SUCCESS_KEY, false).toString()
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}