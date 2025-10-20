package com.pluxity.aiot.system.device.event.dto

import com.pluxity.aiot.system.device.event.DeviceEvent

data class DeviceEventResponse(
    val id: Long,
    val name: String,
    val deviceLevel: String,
)

fun DeviceEvent.toDeviceEventInfo() =
    DeviceEventResponse(
        id = this.id!!,
        name = this.name,
        deviceLevel = this.deviceLevel.toString(),
    )
