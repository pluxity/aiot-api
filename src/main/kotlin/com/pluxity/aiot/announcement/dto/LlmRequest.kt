package com.pluxity.aiot.announcement.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class LlmRequest(
    @field:JsonProperty("yesterday_temp")
    val yesterdayTemp: Double,
    @field:JsonProperty("today_temp")
    val todayTemp: Double,
)
