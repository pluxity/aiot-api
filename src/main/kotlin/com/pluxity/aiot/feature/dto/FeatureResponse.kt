package com.pluxity.aiot.feature.dto

import com.pluxity.aiot.facility.dto.FacilityResponse
import com.pluxity.aiot.facility.dto.toFacilityResponse
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import com.pluxity.aiot.system.device.type.dto.toDeviceTypeResponse

data class FeatureResponse(
    val id: Long,
    val deviceType: DeviceTypeResponse,
    val deviceId: String,
    val objectId: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val batteryLevel: Int?,
    val eventStatus: String,
    val isActive: Boolean,
    val facilityResponse: FacilityResponse?,
)

fun Feature.toFeatureResponse(fileMap: Map<Long, FileResponse>) =
    FeatureResponse(
        id = this.id!!,
        deviceType = this.deviceType!!.toDeviceTypeResponse(fileMap),
        deviceId = this.deviceId,
        objectId = this.objectId,
        name = this.name!!,
        longitude = this.longitude!!,
        latitude = this.latitude!!,
        batteryLevel = this.batteryLevel,
        eventStatus = this.eventStatus!!,
        isActive = this.isActive!!,
        facilityResponse = this.facility?.toFacilityResponse(),
    )
