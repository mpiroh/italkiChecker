package com.apirox.italkichecker

data class ScheduleResponse(val meta: Meta, val data: Data)

data class Meta(val performance: Int, val server_time: Int, val ver: String)

data class Data(val user_timezone: String, val start_datetime: String, val available_schedule: List<List<Double>>)