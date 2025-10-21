package com.pluxity.aiot.system.device.event.dto

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.event.setting.dto.EventConditionResponse
import com.pluxity.aiot.system.event.setting.dto.toEventConditionResponse

data class DeviceEventResponse(
    val id: Long,
    val name: String,
    val eventConditions: List<EventConditionResponse>,
)

fun DeviceEvent.toDeviceEventInfo() =
    DeviceEventResponse(
        id = this.id!!,
        name = this.name,
        eventConditions = this.eventConditions.map { it.toEventConditionResponse() },
    )
