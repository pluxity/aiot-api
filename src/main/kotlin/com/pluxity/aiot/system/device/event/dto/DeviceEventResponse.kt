package com.pluxity.aiot.system.device.event.dto

import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.system.device.event.DeviceEvent
import kotlin.collections.get

data class DeviceEventResponse(
    val id: Long,
    val name: String,
    val deviceLevel: String,
    val iconFile: FileResponse?,
)

fun DeviceEvent.toDeviceEventInfo(fileMap: Map<Long, FileResponse>) =
    DeviceEventResponse(
        id = this.id!!,
        name = this.name,
        deviceLevel = this.deviceLevel.toString(),
        iconFile = fileMap[this.iconId],
    )
