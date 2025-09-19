package com.pluxity.aiot.system.device.type.dto

import com.pluxity.aiot.system.device.event.DeviceEvent

data class DeviceTypeRequest(
    val objectId: String,
    val description: String,
    val version: String,
    val deviceEvents: List<DeviceEventRequest>?,
    val deviceProfileTypes: List<DeviceProfileTypeRequest>?,
)

data class DeviceProfileTypeRequest(
    val profileId: Long,
    val minValue: Double?,
    val maxValue: Double?,
)

data class DeviceEventRequest(
    val id: Long?,
    val name: String,
    val deviceLevel: DeviceEvent.DeviceLevel,
    val iconId: Long?,
)
