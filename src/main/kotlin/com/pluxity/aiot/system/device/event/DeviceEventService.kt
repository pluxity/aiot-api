package com.pluxity.aiot.system.device.event

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.event.dto.DeviceEventRequest
import com.pluxity.aiot.system.device.event.dto.DeviceEventResponse
import com.pluxity.aiot.system.device.event.dto.toDeviceEventInfo
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class DeviceEventService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val em: EntityManager,
) {
    @Transactional(readOnly = true)
    fun findAllByDeviceTypeId(deviceTypeId: Long): List<DeviceEventResponse> {
        val deviceType = findDeviceTypeById(deviceTypeId)
        return deviceType.deviceEvents.map { it.toDeviceEventInfo() }
    }

    @Transactional(readOnly = true)
    fun findById(
        deviceTypeId: Long,
        eventId: Long,
    ): DeviceEventResponse {
        val deviceType = findDeviceTypeById(deviceTypeId)
        val event =
            deviceType.deviceEvents.find { it.id == eventId }
                ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_EVENT, eventId)
        return event.toDeviceEventInfo()
    }

    @Transactional
    fun createOrUpdate(
        deviceTypeId: Long,
        request: DeviceEventRequest,
    ): DeviceEventResponse {
        val deviceType = findDeviceTypeById(deviceTypeId)

        val event: DeviceEvent
        val condition: EventCondition

        if (request.id != null) {
            // 기존 DeviceEvent 수정
            log.info { "DeviceEvent 수정 시작 - ID: ${request.id}" }

            event =
                deviceType.deviceEvents.find { it.id == request.id }
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_EVENT, request.id)

            event.update(request.name, request.deviceLevel)

            // EventCondition 수정
            val conditionRequest = request.eventConditionRequest
            if (conditionRequest.id != null) {
                condition =
                    eventConditionRepository.findByIdOrNull(conditionRequest.id)
                        ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_CONDITION, conditionRequest.id)

                condition.update(
                    isActivate = conditionRequest.isActivate,
                    needControl = conditionRequest.needControl,
                    isBoolean = conditionRequest.isBoolean,
                    minValue = conditionRequest.minValue,
                    maxValue = conditionRequest.maxValue,
                    notificationEnabled = conditionRequest.notificationEnabled,
                    notificationIntervalMinutes = conditionRequest.notificationIntervalMinutes,
                    order = conditionRequest.order,
                )
            } else {
                throw CustomException(ErrorCode.NOT_FOUND_EVENT_CONDITION)
            }

            log.info { "DeviceEvent 수정 완료 - ID: ${request.id}" }
        } else {
            // 새 DeviceEvent 생성
            log.info { "DeviceEvent 생성 시작 - Name: ${request.name}" }

            event =
                DeviceEvent(
                    name = request.name,
                    deviceLevel = request.deviceLevel,
                )

            deviceType.addDeviceEvent(event)
            em.flush() // event.id 생성을 위해 flush

            // EventCondition 생성
            val conditionRequest = request.eventConditionRequest
            condition =
                EventCondition(
                    deviceEvent = event,
                    isActivate = conditionRequest.isActivate,
                    needControl = conditionRequest.needControl,
                    isBoolean = conditionRequest.isBoolean,
                    minValue = conditionRequest.minValue,
                    maxValue = conditionRequest.maxValue,
                    notificationEnabled = conditionRequest.notificationEnabled,
                    notificationIntervalMinutes = conditionRequest.notificationIntervalMinutes ?: 0,
                    order = conditionRequest.order,
                )
            eventConditionRepository.save(condition)

            log.info { "DeviceEvent 생성 완료 - ID: ${event.id}" }
        }

        return event.toDeviceEventInfo()
    }

    @Transactional
    fun delete(
        deviceTypeId: Long,
        eventId: Long,
    ) {
        log.info { "DeviceEvent 삭제 시작 - ID: $eventId" }

        val deviceType = findDeviceTypeById(deviceTypeId)
        val event =
            deviceType.deviceEvents.find { it.id == eventId }
                ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_EVENT, eventId)

        // EventCondition은 orphanRemoval에 의해 자동 삭제됨
        deviceType.deviceEvents.remove(event)

        log.info { "DeviceEvent 삭제 완료 - ID: $eventId" }
    }

    private fun findDeviceTypeById(id: Long): DeviceType =
        deviceTypeRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_TYPE, id)
}
