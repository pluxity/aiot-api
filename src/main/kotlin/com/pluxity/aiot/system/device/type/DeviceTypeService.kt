package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import com.pluxity.aiot.system.device.type.dto.toDeviceTypeResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceTypeService {
    @Transactional(readOnly = true)
    fun findAll(): List<DeviceTypeResponse> = SensorType.entries.map { it.toDeviceTypeResponse() }
}
