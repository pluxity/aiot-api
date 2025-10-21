package com.pluxity.aiot.feature.dto

import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import com.pluxity.aiot.system.device.type.dto.toDeviceTypeResponse

data class FeatureResponse(
    val id: Long,
    val deviceId: String,
    val objectId: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val batteryLevel: Int?,
    val eventStatus: String,
    val isActive: Boolean,
    val height: Double?,
    val siteResponse: SiteResponse?,
    val deviceTypeResponse: DeviceTypeResponse,
)

fun Feature.toFeatureResponse() =
    FeatureResponse(
        id = this.id!!,
        deviceId = this.deviceId,
        objectId = this.objectId.take(5),
        name = this.name!!,
        longitude = this.longitude!!,
        latitude = this.latitude!!,
        batteryLevel = this.batteryLevel,
        eventStatus = this.eventStatus!!,
        isActive = this.isActive!!,
        height = this.height,
        siteResponse = this.site?.toSiteResponse(),
        deviceTypeResponse = SensorType.fromObjectId(this.objectId.take(5)).toDeviceTypeResponse(),
    )
