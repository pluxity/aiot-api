package com.pluxity.aiot.system.device.type.dto

import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.system.device.type.dto.DeviceProfileResponse

data class DeviceTypeResponse(
    var id: Long,
    var objectId: String,
    var description: String,
    var version: String,
    var profiles: List<DeviceProfileResponse>,
)

fun SensorType.toDeviceTypeResponse() =
    DeviceTypeResponse(
        id = this.id,
        objectId = this.objectId,
        description = this.description,
        version = this.version,
        profiles = this.deviceProfiles.map { it.toResponse() },
    )
