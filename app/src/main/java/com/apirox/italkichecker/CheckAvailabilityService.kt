package com.apirox.italkichecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

const val STOP_SERVICE = "stopService"
const val PREFERENCES_NAME = "preferences"
const val SCHEDULE_KEY = "schedule"
const val LAST_CHECK_TIME_KEY = "lastCheckTime"
const val LAST_CHECK_SUCCESS_KEY = "lastCheckSuccess"

class CheckAvailabilityService : Service() {
    private val baseUrl = "https://api.italki.com/api/v2/teacher/3498549/"
    private var timer: Timer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_SERVICE) {
            timer?.cancel()
            timer = null

            stopForeground(true)
            stopSelf()
        } else {
            start()

            val channelId = "serviceChannelId"
            val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

            val notification = Notification.Builder(this, channelId)
                .setContentText("Italki Checker")
                .setContentTitle("Italki Checker running in the background")
                .build()

            startForeground(1001, notification)
        }

        return START_NOT_STICKY
    }

    private fun start() {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val jsonPlaceholderApi = retrofit.create(JsonPlaceholderApi::class.java)

        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                checkAvailability(jsonPlaceholderApi)
            }
        }, 0, 30 * 60 * 1000)     // every 30 minutes
    }

    private fun checkAvailability(jsonPlaceholderApi: JsonPlaceholderApi) {
        val call = jsonPlaceholderApi.getSchedule()
        call.enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                val preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

                val currentTimeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                preferences.edit().putString(LAST_CHECK_TIME_KEY, currentTimeString).apply()

                if (!response.isSuccessful) {
                    preferences.edit().putBoolean(LAST_CHECK_SUCCESS_KEY, false).apply()
                    return
                }
                preferences.edit().putBoolean(LAST_CHECK_SUCCESS_KEY, true).apply()

                val scheduleResponse: ScheduleResponse? = response.body()
                val schedule = scheduleResponse?.data?.available_schedule
                val formattedSchedule = formatSchedule(schedule)

                val oldSchedule = preferences.getString(SCHEDULE_KEY, "") ?: ""
                if (oldSchedule != formattedSchedule) {
                    showNotification(oldSchedule, formattedSchedule)
                }

                preferences.edit().putString(SCHEDULE_KEY, formatSchedule(schedule)).apply()
            }

            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {}
        })
    }

    private fun showNotification(oldSchedule: String, newSchedule: String) {
        val channelId = "scheduleChangedChannelId"
        val channel = NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Italki Checker")
            .setStyle(Notification.BigTextStyle().bigText("Old schedule:\n$oldSchedule\n\nNew schedule:\n$newSchedule"))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1002, notification)
    }

    private fun formatSchedule(schedule: List<List<Double>>?): String {
        return schedule?.map { list ->
            list.map { hours ->
                val stringHours = hours.toString()
                if (stringHours[2] == '0') {
                    hours.toString().substring(0, 1)
                } else {
                    hours.toString()
                }
            }
        }.toString()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}