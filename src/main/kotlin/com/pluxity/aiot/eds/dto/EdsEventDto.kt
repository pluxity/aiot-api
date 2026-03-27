package com.pluxity.aiot.eds.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdsEventData(
    val index: Long,
    val id: Int,
    @field:JsonProperty("profile_name")
    val profileName: String,
    @field:JsonProperty("camera_id")
    val cameraId: String,
    val type: Int,
    @field:JsonProperty("event_start")
    val eventStart: String,
    @field:JsonProperty("event_end")
    val eventEnd: String? = null,
    @field:JsonProperty("frame_time")
    val frameTime: String? = null,
    val status: Int,
    @field:JsonProperty("event_zone_id")
    val eventZoneId: Int? = null,
    @field:JsonProperty("event_zone_name")
    val eventZoneName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @field:JsonProperty("detected_vehicle_number")
    val detectedVehicleNumber: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdsCrowdCountData(
    @field:JsonProperty("camera_id")
    val cameraId: String,
    @field:JsonProperty("frame_time")
    val frameTime: String,
    val total: Int,
    @field:JsonProperty("zone_num")
    val zoneNum: Int,
    val zones: List<EdsCrowdZone>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EdsCrowdZone(
    val index: Long? = null,
    @field:JsonProperty("profile_name")
    val profileName: String? = null,
    @field:JsonProperty("event_zone_id")
    val eventZoneId: Long? = null,
    @field:JsonProperty("event_zone_name")
    val eventZoneName: String? = null,
    @field:JsonProperty("event_zone_level")
    val eventZoneLevel: Int? = null,
    @field:JsonProperty("event_zone_extent")
    val eventZoneExtent: Int? = null,
    @field:JsonProperty("event_zone_cnt")
    val eventZoneCnt: Int? = null,
    @field:JsonProperty("event_zone_avg")
    val eventZoneAvg: Double? = null,
)

data class EdsWebSocketUrlResult(
    @field:JsonProperty("ws_url")
    val wsUrl: String,
)
