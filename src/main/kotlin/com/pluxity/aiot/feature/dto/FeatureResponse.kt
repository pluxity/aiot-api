package com.pluxity.aiot.feature.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.sensor.type.dto.DeviceTypeResponse
import com.pluxity.aiot.sensor.type.dto.toDeviceTypeResponse
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse

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
    @field:JsonUnwrapped var baseResponse: BaseResponse,
)

fun Feature.toFeatureResponse() =
    FeatureResponse(
        id = this.requiredId,
        deviceId = this.deviceId,
        objectId = this.objectId.take(5),
        name = requireNotNull(this.name) { "Feature(${this.id}) name is null (not ready)" },
        longitude = requireNotNull(this.longitude) { "Feature(${this.id}) longitude is null (not ready)" },
        latitude = requireNotNull(this.latitude) { "Feature(${this.id}) latitude is null (not ready)" },
        batteryLevel = this.batteryLevel,
        eventStatus = this.eventStatus,
        isActive = this.isActive,
        height = this.height,
        siteResponse = this.site?.toSiteResponse(),
        deviceTypeResponse = SensorType.fromObjectId(this.objectId.take(5)).toDeviceTypeResponse(),
        baseResponse = this.toBaseResponse(),
    )
