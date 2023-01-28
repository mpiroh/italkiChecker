package com.apirox.italkichecker

import retrofit2.Call
import retrofit2.http.GET

interface JsonPlaceholderApi {
    @GET("simple_schedule?user_timezone=Europe/Prague&closest_available_datetime_type=1&with_half_hour=1")
    fun getSchedule(): Call<ScheduleResponse>
}