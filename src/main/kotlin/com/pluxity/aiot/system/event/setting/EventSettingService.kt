package com.pluxity.aiot.system.event.setting

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.event.DeviceEventRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.profile.DeviceProfileTypeRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.dto.EventConditionRequest
import com.pluxity.aiot.system.event.setting.dto.EventSettingHistoryResponse
import com.pluxity.aiot.system.event.setting.dto.EventSettingRequest
import com.pluxity.aiot.system.event.setting.dto.EventSettingResponse
import com.pluxity.aiot.system.event.setting.dto.toEventSettingHistoryResponse
import com.pluxity.aiot.system.event.setting.dto.toEventSettingResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class EventSettingService(
    private val eventSettingRepository: EventSettingRepository,
    private val deviceProfileTypeRepository: DeviceProfileTypeRepository,
    private val deviceEventRepository: DeviceEventRepository,
    private val eventSettingHistoryRepository: EventSettingHistoryRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    // TODO 센서종류 추가되면 캐시에 담는 로직 추가 필요
//        private val sensorDataHandler: SensorDataHandler,
    private val em: EntityManager,
) {
    private fun findDeviceEvent(id: Long) =
        deviceEventRepository.findByIdWithDeviceType(id) ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_EVENT, id)

    private fun findDeviceProfileById(id: Long) =
        deviceProfileTypeRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_PROFILE, id)

    private fun findById(id: Long) =
        eventSettingRepository.findWithConditionsById(id) ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_SETTING, id)

    @Transactional(readOnly = true)
    fun getById(id: Long): EventSettingResponse = findById(id).toEventSettingResponse()

    @Transactional(readOnly = true)
    fun findAll(): List<EventSettingResponse> = eventSettingRepository.findAll().map { it.toEventSettingResponse() }

    @Transactional
    fun delete(id: Long) {
        findById(id)
        // 먼저 이력 삭제
        eventSettingHistoryRepository.deleteAllByEventSettingId(id)
        // 그 다음 이벤트 설정 삭제
        eventSettingRepository.deleteById(id)
    }

    @Transactional
    fun updateEventSetting(request: EventSettingRequest) {
        val deviceProfileType: DeviceProfileType = findDeviceProfileById(request.deviceProfileTypeId)

        // id로 EventSetting 찾기
        val eventSetting = findById(request.id)

        // eventEnabled 상태 업데이트
        eventSetting.updateEventEnabled(request.eventEnabled)

        // 기간 설정 업데이트
        eventSetting.updatePeriodic(request.isPeriodic)
        if (request.isPeriodic) {
            eventSetting.updateMonths(request.months)
        } else {
            eventSetting.updateMonths(null) // 전체 기간인 경우 months를 null로 설정
        }

        // 조건 범위 중복 검증
        validateConditionRanges(request.conditions)

        // 기존 conditions 모두 가져오기
        val existingConditions: List<EventCondition> = eventSetting.conditions.toList()
        var hasChanges = false

        // 요청된 order를 기준으로 조건을 업데이트
        for (conditionRequest in request.conditions) {
            val deviceEvent: DeviceEvent = findDeviceEvent(conditionRequest.deviceEventId)

            // 기존 condition 찾기
            val existingCondition: EventCondition =
                existingConditions
                    .firstOrNull { condition -> condition.deviceEvent.id!! == deviceEvent.id }
                    ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_CONDITION, deviceEvent.id)

            // 값이나 설정이 변경되었는지 확인
            if (existingCondition.value != conditionRequest.value ||
                existingCondition.notificationEnabled != conditionRequest.notificationEnabled ||
                existingCondition.locationTrackingEnabled != conditionRequest.locationTrackingEnabled ||
                existingCondition.soundEnabled != conditionRequest.soundEnabled ||
                existingCondition.fireEffectEnabled != conditionRequest.fireEffectEnabled ||
                existingCondition.controlType != conditionRequest.controlType ||
                existingCondition.guideMessage != conditionRequest.guideMessage ||
                existingCondition.notificationIntervalMinutes != conditionRequest.notificationIntervalMinutes ||
                existingCondition.order != conditionRequest.order
            ) {
                hasChanges = true
            }

            // condition 업데이트
            existingCondition.update(
                conditionRequest.value, // 프론트엔드에서 전달한 value 그대로 사용
                conditionRequest.operator,
                conditionRequest.notificationEnabled,
                conditionRequest.locationTrackingEnabled,
                conditionRequest.soundEnabled,
                conditionRequest.fireEffectEnabled,
                conditionRequest.controlType,
                conditionRequest.guideMessage,
                conditionRequest.notificationIntervalMinutes,
                conditionRequest.order, // 프론트엔드에서 전달한 order 그대로 사용
            )
        }

        // 변경사항이 있으면 이력 저장
        if (hasChanges) {
            val history = EventSettingHistory.of(eventSetting)
            eventSettingHistoryRepository.save(history)
        }

        eventSettingRepository.save(eventSetting)
        updateDeviceTypeCache(deviceProfileType.deviceType.id!!)
    }

    @Transactional(readOnly = true)
    fun getSettingHistories(settingId: Long): List<EventSettingHistoryResponse> =
        eventSettingHistoryRepository
            .findAllByEventSettingIdOrderByUpdatedAtDesc(settingId)
            .map { it.toEventSettingHistoryResponse() }

    @Transactional
    fun cloneWithPeriod(sourceSettingId: Long) {
        // 원본 설정 조회
        val sourceSetting = findById(sourceSettingId)

        // 기존 설정 업데이트 - 선택한 월 설정
        sourceSetting.updatePeriodic(true)
        eventSettingRepository.save(sourceSetting)

        // 새로운 설정 생성 - 월 없이 생성
        val newSetting =
            EventSetting(
                deviceProfileType = sourceSetting.deviceProfileType,
                isPeriodic = true,
                // 월 없이 생성
                months = null,
            )

        // 조건 목록 생성하여 범위 중복 확인
        val conditionRequests: List<EventConditionRequest> =
            sourceSetting.conditions
                .map { condition: EventCondition ->
                    EventConditionRequest(
                        null,
                        condition.deviceEvent.id!!,
                        condition.value,
                        condition.operator,
                        condition.notificationEnabled,
                        condition.locationTrackingEnabled,
                        condition.soundEnabled,
                        condition.fireEffectEnabled,
                        condition.controlType,
                        condition.guideMessage,
                        condition.notificationIntervalMinutes,
                        condition.order,
                    )
                }

        validateConditionRanges(conditionRequests)

        // 조건 복사
        sourceSetting.conditions.forEach { condition: EventCondition ->
            val newCondition =
                EventCondition(
                    deviceEvent = condition.deviceEvent,
                    operator = condition.operator,
                    value = condition.value,
                    minValue = condition.minValue,
                    maxValue = condition.maxValue,
                    notificationEnabled = condition.notificationEnabled,
                    locationTrackingEnabled = condition.locationTrackingEnabled,
                    soundEnabled = condition.soundEnabled,
                )
            newSetting.addCondition(newCondition)
        }

        eventSettingRepository.save(newSetting)
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    fun updateDeviceTypeCache(deviceTypeId: Long) {
        em.flush()
        em.clear()
        val deviceType =
            deviceTypeRepository.findByIdOrNull(deviceTypeId)
                ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_TYPE, deviceTypeId)
        // TODO
//        sensorDataHandler.updateDeviceTypeCache(deviceType)
        log.debug { "Device type cache updated for device type: ${deviceType.objectId}" }
    }

    // 조건 범위 중복 검증 메소드 추가
    private fun validateConditionRanges(conditions: List<EventConditionRequest>) {
        // 범위 연산자(BETWEEN)를 사용하는 조건만 필터링
        val rangeConditions: List<EventConditionRequest> =
            conditions
                .filter { c -> c.operator === EventCondition.ConditionOperator.BETWEEN }

        // 조건이 2개 미만이면 검증 불필요
        if (rangeConditions.size < 2) {
            return
        }

        // 모든 조합에 대해 범위 중복 확인
        for (i in 0..<rangeConditions.size - 1) {
            for (j in i + 1..<rangeConditions.size) {
                val condA: EventConditionRequest = rangeConditions[i]
                val condB: EventConditionRequest = rangeConditions[j]

                if (isRangeOverlapping(condA.value, condB.value)) {
                    val eventA: DeviceEvent = findDeviceEvent(condA.deviceEventId)
                    val eventB: DeviceEvent = findDeviceEvent(condB.deviceEventId)
                    throw CustomException(
                        ErrorCode.DUPLICATE_EVENT_CONDITION,
                        eventA.name,
                        condA.value,
                        eventB.name,
                        condB.value,
                    )
                }
            }
        }
    }

    // 두 범위가 중복되는지 확인하는 메소드
    private fun isRangeOverlapping(
        rangeA: String,
        rangeB: String,
    ): Boolean {
        try {
            val partsA = rangeA.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val partsB = rangeB.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (partsA.size != 2 || partsB.size != 2) {
                return false // 형식이 잘못된 경우
            }

            val minA = partsA[0].toDouble()
            val maxA = partsA[1].toDouble()
            val minB = partsB[0].toDouble()
            val maxB = partsB[1].toDouble()

            // 범위 중복 확인: 한 범위의 최댓값이 다른 범위의 최솟값보다 작거나 같지 않으면 중복
            return maxOf(minA, minB) <= minOf(maxA, maxB)
        } catch (_: NumberFormatException) {
            return false // 숫자 변환 실패 또는 배열 인덱스 오류
        } catch (_: ArrayIndexOutOfBoundsException) {
            return false
        }
    }
}
