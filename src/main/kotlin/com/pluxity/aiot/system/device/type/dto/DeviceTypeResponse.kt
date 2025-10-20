package com.pluxity.aiot.system.device.type.dto

import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.system.device.event.dto.DeviceEventResponse
import com.pluxity.aiot.system.device.event.dto.toDeviceEventInfo
import com.pluxity.aiot.system.device.profile.dto.DeviceProfileResponse
import com.pluxity.aiot.system.device.type.DeviceType

data class DeviceTypeResponse(
    var id: Long,
    var objectId: String,
    var description: String,
    var version: String,
    var events: List<DeviceEventResponse>,
    var profiles: List<DeviceProfileResponse>
)

fun DeviceType.toDeviceTypeResponse(fileMap: Map<Long, FileResponse>) =
    DeviceTypeResponse(
        id = this.id!!,
        objectId = this.objectId,
        description = this.description!!,
        version = this.version!!,
        events = this.deviceEvents.map { it.toDeviceEventInfo(fileMap) },
        profiles = this.deviceProfiles.map { it.toResponse() },
    )
