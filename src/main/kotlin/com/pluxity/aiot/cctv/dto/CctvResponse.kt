package com.pluxity.aiot.cctv.dto

import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.eds.EdsCameraStatus
import com.pluxity.aiot.eds.EdsCameraType
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse

data class CctvResponse(
    val id: Long,
    val name: String,
    val edsCameraId: String,
    val cameraIp: String? = null,
    val cameraPort: Int? = null,
    val lon: Double?,
    val lat: Double?,
    val ptzControl: Int? = null,
    val cameraType: EdsCameraType? = null,
    val cameraStatus: EdsCameraStatus? = null,
    val cameraAddress: String? = null,
    val streamResolution: List<Int>? = null,
    val cameraRecordType: Int? = null,
    val cameraAnalysisConfigured: Int? = null,
    val site: SiteResponse? = null,
)

fun Cctv.toCctvResponse() =
    CctvResponse(
        id = this.requiredId,
        name = this.name,
        edsCameraId = this.edsCameraId,
        cameraIp = this.cameraIp,
        cameraPort = this.cameraPort,
        lon = this.longitude,
        lat = this.latitude,
        ptzControl = this.ptzControl,
        cameraType = this.cameraType,
        cameraStatus = this.cameraStatus,
        cameraAddress = this.cameraAddress,
        streamResolution =
            this.streamResolutionWidth?.let { w ->
                this.streamResolutionHeight?.let { h -> listOf(w, h) }
            },
        cameraRecordType = this.cameraRecordType,
        cameraAnalysisConfigured = this.cameraAnalysisConfigured,
        site = this.site?.toSiteResponse(),
    )
