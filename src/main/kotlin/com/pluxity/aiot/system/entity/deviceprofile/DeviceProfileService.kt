package com.pluxity.aiot.system.entity.deviceprofile

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.entity.deviceprofile.dto.DeviceProfileRequest
import com.pluxity.aiot.system.entity.deviceprofile.dto.DeviceProfileResponse
import com.pluxity.aiot.system.entity.deviceprofile.dto.toDeviceProfileResponse
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DeviceProfileService(
    private val deviceProfileRepository: DeviceProfileRepository,
) {
    @Transactional
    fun create(request: DeviceProfileRequest): Long {
        // 필드명 중복 검증
        if (deviceProfileRepository.existsByFieldKey(request.fieldKey)) {
            throw CustomException(ErrorCode.DUPLICATE_DEVICE_PROFILE_KEY, request.fieldKey)
        }

        val deviceProfile =
            DeviceProfile(
                fieldKey = request.fieldKey,
                description = request.description,
                fieldUnit = request.fieldUnit,
                fieldType = request.fieldType,
            )

        return deviceProfileRepository.save(deviceProfile).id!!
    }

    fun findAll(): List<DeviceProfileResponse> = deviceProfileRepository.findAll().map { it.toDeviceProfileResponse() }

    private fun findById(id: Long): DeviceProfile =
        deviceProfileRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_PROFILE, id)

    @Transactional
    fun update(
        id: Long,
        request: DeviceProfileRequest,
    ) {
        val deviceProfile = findById(id)

        // 필드명 중복 검증 (자기 자신은 제외)
        if (deviceProfileRepository.existsByFieldKeyAndIdNot(request.fieldKey, id)) {
            throw CustomException(ErrorCode.DUPLICATE_DEVICE_PROFILE_KEY, request.fieldKey)
        }

        deviceProfile.update(
            request.fieldKey,
            request.description,
            request.fieldUnit,
            request.fieldType,
        )
    }

    @Transactional
    fun delete(id: Long) {
        findById(id)
        deviceProfileRepository.deleteById(id)
    }
}
