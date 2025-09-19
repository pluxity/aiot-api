package com.pluxity.aiot.system.device.profile.dto

import com.pluxity.aiot.system.device.profile.DeviceProfile

data class DeviceProfileResponse(
    val id: Long,
    val fieldKey: String,
    val description: String,
    val fieldUnit: String?,
    val fieldType: DeviceProfile.FieldType,
)

fun DeviceProfile.toDeviceProfileResponse() =
    DeviceProfileResponse(
        id = this.id!!,
        fieldKey = this.fieldKey,
        description = this.description,
        fieldUnit = this.fieldUnit,
        fieldType = this.fieldType,
    )
