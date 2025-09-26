package com.pluxity.aiot.data.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class LocationData(
    val latitude: Double,
    val longitude: Double,
)

data class MobiusBatteryResponse(
    @field:JsonProperty("m2m:cin")
    val cin: MobiusCinResponse,
)

data class MobiusCinResponse(
    val con: MobiusConResponse,
)

data class MobiusConResponse(
    @field:JsonProperty("Battery Level")
    val batteryLevel: Int,
)

data class MobiusUrilResponse(
    @field:JsonProperty("m2m:uril")
    val uril: List<String>,
)

data class MobiusLocationResponse(
    @field:JsonProperty("m2m:cnt")
    val cntResponse: MobiusCntResponse,
)

data class MobiusCntResponse(
    val lbl: List<String>,
)

data class SubscriptionRequest(
    @field:JsonProperty("m2m:sub")
    val sub: SubscriptionM2mSub,
)

data class SubscriptionM2mSub(
    val rn: String,
    val enc: SubscriptionEnc = SubscriptionEnc(),
    val nct: Int = 1,
    val nu: List<String>,
)

data class SubscriptionEnc(
    val net: List<Int> = listOf(3),
)
