package com.pluxity.aiot.system.device.event.dto

import com.pluxity.aiot.system.event.setting.dto.EventConditionRequest

data class DeviceEventRequest(
    val id: Long?,
    val name: String,
    val eventConditionRequest: EventConditionRequest,
)
