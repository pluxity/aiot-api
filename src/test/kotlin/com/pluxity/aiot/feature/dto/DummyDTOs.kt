package com.pluxity.aiot.feature.dto

import com.pluxity.aiot.sensor.type.FieldType
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.dummySiteResponse
import com.pluxity.aiot.system.device.type.dto.DeviceProfileResponse
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse

fun dummyFeatureResponse(
    id: Long = 1L,
    deviceId: String = "TH001-34954-id",
    objectId: String = "34954",
    name: String = "Test Feature",
    longitude: Double = 127.0,
    latitude: Double = 37.0,
    batteryLevel: Int? = null,
    eventStatus: String = "NORMAL",
    isActive: Boolean = true,
    height: Double? = null,
    siteResponse: SiteResponse? = dummySiteResponse(),
    deviceTypeResponse: DeviceTypeResponse = dummyDeviceTypeResponse(),
): FeatureResponse =
    FeatureResponse(
        id = id,
        deviceId = deviceId,
        objectId = objectId,
        name = name,
        latitude = latitude,
        longitude = longitude,
        batteryLevel = batteryLevel,
        eventStatus = eventStatus,
        isActive = isActive,
        height = height,
        siteResponse = siteResponse,
        deviceTypeResponse = deviceTypeResponse,
    )

fun dummyDeviceTypeResponse(
    id: Long = 1L,
    objectId: String = "34954",
    description: String = "온도계",
    version: String = "1.0",
    profiles: List<DeviceProfileResponse> = listOf(dummyDeviceProfileResponse()),
) = DeviceTypeResponse(
    id = id,
    objectId = objectId,
    description = description,
    version = version,
    profiles = profiles,
)

fun dummyDeviceProfileResponse(
    id: Long = 1L,
    fieldKey: String = "Temperature",
    description: String = "온도",
    fieldUnit: String? = "°C",
    fieldType: FieldType = FieldType.Float,
) = DeviceProfileResponse(
    id = id,
    fieldKey = fieldKey,
    description = description,
    fieldUnit = fieldUnit,
    fieldType = fieldType,
)
