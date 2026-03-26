package com.pluxity.aiot.eds.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EdsRealtimeStreamRequest(
    @field:JsonProperty("camera_id")
    val cameraId: String,
    @field:JsonProperty("external_flag")
    val externalFlag: Int = 2,
    @field:JsonProperty("meta_include_flag")
    val metaIncludeFlag: Int = 0,
    @field:JsonProperty("protocol_type")
    val protocolType: String = "ws",
)

data class EdsRecordStreamRequest(
    @field:JsonProperty("camera_id")
    val cameraId: String,
    @field:JsonProperty("external_flag")
    val externalFlag: Int = 2,
    @field:JsonProperty("protocol_type")
    val protocolType: String = "ws",
    @field:JsonProperty("record_start_time")
    val recordStartTime: String,
    @field:JsonProperty("record_end_time")
    val recordEndTime: String,
)

data class EdsStreamResult(
    @field:JsonProperty("stream_url")
    val streamUrl: String,
    val session: String? = null,
)
