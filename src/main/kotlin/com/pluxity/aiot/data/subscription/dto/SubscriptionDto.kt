package com.pluxity.aiot.data.subscription.dto

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
    @field:JsonProperty("ContainerModuleId")
    val containerModuleId: Int? = null,
    @field:JsonProperty("ActualFilling")
    val actualFilling: Int? = null,
    @field:JsonProperty("HighThreshold")
    val highThreshold: Int? = null,
)

data class SubscriptionRepListResponse(
    @field:JsonProperty("m2m:cin")
    val cin: List<SubscriptionCinResponse>?,
)
