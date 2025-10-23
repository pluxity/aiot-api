package com.pluxity.aiot.system.device.type.dto

import com.pluxity.aiot.sensor.type.FieldType

data class DeviceProfileResponse(
    val id: Long,
    val fieldKey: String,
    val description: String,
    val fieldUnit: String?,
    val fieldType: FieldType,
)
