package com.pluxity.aiot.eds.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EdsCameraInfo(
    @field:JsonProperty("camera_id")
    val cameraId: String,
    @field:JsonProperty("camera_name")
    val cameraName: String,
    @field:JsonProperty("camera_ip")
    val cameraIp: String,
    @field:JsonProperty("camera_port")
    val cameraPort: Int,
    val latitude: Double?,
    val longitude: Double?,
    @field:JsonProperty("ptz_control")
    val ptzControl: Int,
    @field:JsonProperty("camera_type")
    val cameraType: String,
    @field:JsonProperty("camera_status")
    val cameraStatus: Int,
    @field:JsonProperty("camera_address")
    val cameraAddress: String? = null,
    @field:JsonProperty("stream_resolution")
    val streamResolution: List<Int>? = null,
    @field:JsonProperty("camera_record_type")
    val cameraRecordType: Int? = null,
    @field:JsonProperty("camera_analysis_configured")
    val cameraAnalysisConfigured: Int? = null,
    @field:JsonProperty("camera_install_purpose")
    val cameraInstallPurpose: String? = null,
)

data class EdsCameraListResponse(
    val code: Int,
    val message: String,
    @field:JsonProperty("time_stamp")
    val timeStamp: String? = null,
    @field:JsonProperty("total_count")
    val totalCount: Int? = null,
    val result: List<EdsCameraInfo>? = null,
)
