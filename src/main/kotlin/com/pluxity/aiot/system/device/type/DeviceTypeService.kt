package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.profile.dto.DeviceProfileResponse
import com.pluxity.aiot.system.device.profile.dto.toDeviceProfileResponse
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import com.pluxity.aiot.system.device.type.dto.toDeviceTypeResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceTypeService(
    private val deviceTypeRepository: DeviceTypeRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<DeviceTypeResponse> {
        val list = deviceTypeRepository.findAll()
        return list.map { it.toDeviceTypeResponse() }
    }

    fun findById(id: Long) = deviceTypeRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_TYPE, id)

    @Transactional(readOnly = true)
    fun getById(id: Long): DeviceTypeResponse {
        val target = findById(id)
        return target.toDeviceTypeResponse()
    }

    @Transactional(readOnly = true)
    fun findProfilesByDeviceTypeId(id: Long): List<DeviceProfileResponse> {
        val deviceType = findById(id)
        return deviceType.deviceProfileTypes
            .mapNotNull { it.deviceProfile?.toDeviceProfileResponse() }
    }
}
