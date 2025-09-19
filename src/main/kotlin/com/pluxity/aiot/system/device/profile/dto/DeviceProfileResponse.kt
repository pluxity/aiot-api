package com.pluxity.aiot.system.device.profile.dto

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType

data class DeviceProfileResponse(
    val id: Long,
    val fieldKey: String,
    val description: String,
    val fieldUnit: String?,
    val fieldType: DeviceProfile.FieldType,
    val deviceTypes: List<DeviceTypeInfo>,
)

fun DeviceProfile.toDeviceProfileResponse() =
    DeviceProfileResponse(
        id = this.id!!,
        fieldKey = this.fieldKey,
        description = this.description,
        fieldUnit = this.fieldUnit,
        fieldType = this.fieldType,
        deviceTypes = this.deviceProfileTypes.mapNotNull { it.deviceType?.toDeviceTypeInfo(it) },
    )

data class DeviceTypeInfo(
    var id: Long,
    var objectId: String,
    var description: String,
    var version: String,
    var events: List<DeviceEventInfo>,
    var deviceProfileType: DeviceProfileTypeInfo,
)

fun DeviceType.toDeviceTypeInfo(deviceProfileType: DeviceProfileType) =
    DeviceTypeInfo(
        id = this.id!!,
        objectId = this.objectId!!,
        description = this.description!!,
        version = this.version!!,
        events = this.deviceEvents.map { it.toDeviceEventInfo() },
        deviceProfileType = deviceProfileType.toDeviceProfileTypeInfo(),
    )

data class DeviceProfileTypeInfo(
    var id: Long,
    var deviceProfileId: Long,
)

fun DeviceProfileType.toDeviceProfileTypeInfo() =
    DeviceProfileTypeInfo(
        id = this.id!!,
        deviceProfileId = this.deviceProfile!!.id!!,
    )

data class DeviceEventInfo(
    val id: Long,
    val name: String,
    val deviceLevel: String,
    val imageUrl: String?,
)

fun DeviceEvent.toDeviceEventInfo() =
    DeviceEventInfo(
        id = this.id!!,
        name = this.name,
        deviceLevel = this.deviceLevel.toString(),
        imageUrl = this.imageUrl,
    )
