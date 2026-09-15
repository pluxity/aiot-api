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

/** con.Timestamp는 규격상 선택이라 악취 단말은 안 보낸다. 빠지면 cin 생성 시각 ct로 채운다. */
data class SubscriptionCinResponse(
    val ct: String? = null,
    @field:JsonProperty("con")
    private val rawCon: SubscriptionConResponse,
) {
    val con: SubscriptionConResponse
        get() = if (rawCon.reportedTimestamp != null) rawCon else rawCon.copy(reportedTimestamp = ct)
}

data class SubscriptionConResponse(
    @field:JsonProperty("Temperature")
    val temperature: Double?,
    @field:JsonProperty("Humidity")
    val humidity: Double?,
    @field:JsonProperty("Reporting Period")
    val period: Int = 300,
    @field:JsonProperty("Timestamp")
    val reportedTimestamp: String? = null,
    @field:JsonProperty("Fire Alarm")
    val fireAlarm: Boolean?,
    @field:JsonProperty("ContainerModuleId")
    val containerModuleId: Int? = null,
    @field:JsonProperty("ActualFilling")
    val actualFilling: Int? = null,
    @field:JsonProperty("HighThreshold")
    val highThreshold: Int? = null,
    @field:JsonProperty("FireDetection")
    val fireDetection: Boolean? = null,
    @field:JsonProperty("CO2")
    val co2: Int? = null,
    @field:JsonProperty("CO")
    val co: Int? = null,
    @field:JsonProperty("TVOC")
    val tvoc: Int? = null,
    @field:JsonProperty("FireCauseMask")
    val fireCauseMask: Int? = null,
    @field:JsonProperty("NH3")
    val nh3: Int? = null,
    @field:JsonProperty("H2S")
    val h2s: Int? = null,
    @field:JsonProperty("NumberOfVisitors")
    val numberOfVisitors: Int? = null,
    @field:JsonProperty("NumberOfLeavers")
    val numberOfLeavers: Int? = null,
    @field:JsonProperty("PM2.5")
    val pm25: Int? = null,
    @field:JsonProperty("PM10")
    val pm10: Int? = null,
    @field:JsonProperty("WindSpeed")
    val windSpeed: Int? = null,
    @field:JsonProperty("WindDirection")
    val windDirection: Int? = null,
    @field:JsonProperty("UVI")
    val uvi: Int? = null,
    @field:JsonProperty("LED Light")
    val ledLight: Int? = null,
) {
    val timestamp: String
        get() = checkNotNull(reportedTimestamp) { "Timestamp도 ct도 없는 알림" }
}

data class MobiusDataReportResponse(
    @field:JsonProperty("m2m:cnt")
    val cnt: SubscriptionRepListResponse? = null,
)

data class SubscriptionRepListResponse(
    @field:JsonProperty("m2m:cin")
    val cin: List<SubscriptionCinResponse>?,
)
