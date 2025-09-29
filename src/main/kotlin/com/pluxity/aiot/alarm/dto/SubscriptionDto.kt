package com.pluxity.aiot.alarm.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class SubscriptionAlarm(
    @field:JsonProperty("m2m:sgn")
    val sgn: SubscriptionSgnResponse,
)

data class SubscriptionSgnResponse(
    val sur: String = "",
    val nev: SubscriptionNevResponse,
)

data class SubscriptionNevResponse(
    val rep: SubscriptionRepResponse,
)

data class SubscriptionRepResponse(
    @field:JsonProperty("m2m:cin")
    val cin: SubscriptionCinResponse,
)

data class SubscriptionCinResponse(
    val con: SubscriptionConResponse,
)

data class SubscriptionConResponse(
    @field:JsonProperty("Temperature")
    val temperature: Double?,
    @field:JsonProperty("Humidity")
    val humidity: Double?,
    @field:JsonProperty("Reporting Period")
    val period: Int = 300,
    @field:JsonProperty("Timestamp")
    val timestamp: String,
    @field:JsonProperty("Fire Alarm")
    val fireAlarm: Boolean?,
    @field:JsonProperty("Angle-X")
    val angleX: Double?,
    @field:JsonProperty("Angle-Y")
    val angleY: Double?,
)

data class SubscriptionRepListResponse(
    @field:JsonProperty("m2m:cin")
    val cin: List<SubscriptionCinResponse>?,
)
