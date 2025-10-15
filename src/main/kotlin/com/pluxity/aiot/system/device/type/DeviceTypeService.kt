package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.alarm.service.SensorDataHandler
import com.pluxity.aiot.alarm.type.DeviceProfileEnum
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.file.extensions.getFileMapByIds
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.event.DeviceEventRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.profile.dto.DeviceProfileResponse
import com.pluxity.aiot.system.device.profile.dto.toDeviceProfileResponse
import com.pluxity.aiot.system.device.type.dto.DeviceEventRequest
import com.pluxity.aiot.system.device.type.dto.DeviceProfileTypeRequest
import com.pluxity.aiot.system.device.type.dto.DeviceTypeRequest
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import com.pluxity.aiot.system.device.type.dto.toDeviceTypeResponse
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class DeviceTypeService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceEventRepository: DeviceEventRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val sensorDataHandler: SensorDataHandler,
    private val em: EntityManager,
    private val fileService: FileService,
) {
    companion object {
        private const val PREFIX = "device_events/"
    }

    @Transactional(readOnly = true)
    fun findAll(): List<DeviceTypeResponse> {
        val list = deviceTypeRepository.findAll()
        val fileMap =
            fileService.getFileMapByIds(deviceEventRepository.findAll()) {
                listOfNotNull(it.iconId)
            }
        return list.map { it.toDeviceTypeResponse(fileMap) }
    }

    fun findById(id: Long) = deviceTypeRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_TYPE, id)

    @Transactional(readOnly = true)
    fun getById(id: Long): DeviceTypeResponse {
        val target = findById(id)
        val fileMap =
            fileService.getFileMapByIds(deviceEventRepository.findAll()) {
                listOfNotNull(it.iconId)
            }
        return target.toDeviceTypeResponse(fileMap)
    }

    @Transactional(readOnly = true)
    fun findProfilesByDeviceTypeId(id: Long): List<DeviceProfileResponse> {
        val deviceType = findById(id)
        return deviceType.deviceProfileTypes
            .mapNotNull { it.deviceProfile?.toDeviceProfileResponse() }
    }

    private fun validateMinMaxValues(profileSettings: DeviceProfileTypeRequest) {
        val (minValue, maxValue) = profileSettings.minValue to profileSettings.maxValue
        if (minValue != null && maxValue != null) {
            if (minValue > maxValue) {
                throw CustomException(ErrorCode.INVALID_PROFILE_MIN_MAX_VALUE, minValue, maxValue)
            }
        }
    }

    @Transactional
    fun update(
        id: Long,
        request: DeviceTypeRequest,
    ) {
        log.info { "DeviceType 업데이트 시작 - ID: $id" }

        val deviceType = findById(id)

        // 이벤트 업데이트
        if (request.deviceEvents != null) {
            // 기존 이벤트 ID 목록
            val existingEventIds = deviceType.deviceEvents.mapNotNull { it.id }

            // 요청에 포함된 이벤트 ID 목록
            val requestEventIds =
                request.deviceEvents
                    .mapNotNull { it.id }
                    .toSet()

            // 삭제될 이벤트 ID 목록 (기존에 있었지만 요청에는 없는 이벤트)
            val eventsToRemove =
                existingEventIds
                    .filterNot { it in requestEventIds }
                    .toSet()

            // 삭제될 이벤트와 관련된 EventCondition을 먼저 제거
            if (!eventsToRemove.isEmpty()) {
                log.info { "제거될 이벤트 ID: $eventsToRemove" }

                // Repository를 사용하여 EventCondition 삭제
                val deletedCount = eventConditionRepository.deleteAllByDeviceEventIdIn(eventsToRemove)

                log.info { "삭제된 이벤트 조건 수: $deletedCount" }

                // 연관 참조를 정리 (메모리상)
                deviceType.deviceEvents.removeIf { event: DeviceEvent -> eventsToRemove.contains(event.id) }
            }

            // 이벤트 생성 또는 업데이트
            val events = createOrUpdateEvents(request.deviceEvents)

            // 새 이벤트의 iconId를 임시 저장
            val newEventIconIds =
                events
                    .filter { it.id == null && it.iconId != null }
                    .associateWith { it.iconId!! }

            deviceType.updateDeviceEvents(events)

            // 변경 내용 저장 (새 DeviceEvent에 id가 생성됨)
            em.flush()

            // 새로 생성된 DeviceEvent의 iconId 처리
            newEventIconIds.forEach { (event, iconId) ->
                val iconFile = fileService.finalizeUpload(iconId, "${PREFIX}${event.id}/")
                event.updateIconId(iconFile.id)
            }
        }

        // 기존 deviceProfileTypes와 요청(request)의 deviceProfileTypes를 비교하여 동기화
        if (request.deviceProfileTypes != null && !request.deviceProfileTypes.isEmpty()) {
            // 각 프로필 설정의 최소값/최대값 유효성 검사
            request.deviceProfileTypes
                .forEach { this.validateMinMaxValues(it) }

            val existingProfileIds =
                deviceType.deviceProfileTypes
                    .map { it.deviceProfile?.id }
                    .toSet()

            val requestedProfileIds =
                request.deviceProfileTypes
                    .map { it.profileId }
                    .toSet()

            val profileSettingsMap: Map<Long, DeviceProfileTypeRequest> =
                request.deviceProfileTypes.associateBy { it.profileId }

            // 1. 삭제: 기존에 연결되어 있으나 요청에서 제거된 프로필
            val toRemove =
                deviceType.deviceProfileTypes
                    .filterNot { dpt -> requestedProfileIds.contains(dpt.deviceProfile?.id) }

            for (dpt in toRemove) {
                deviceType.removeDeviceProfile(dpt.deviceProfile)
            }

            // 2. 업데이트: 기존 연관 프로필의 EventCondition 조건 업데이트
            for (dpt in deviceType.deviceProfileTypes) {
                val profileId = dpt.deviceProfile?.id
                if (requestedProfileIds.contains(profileId)) {
                    val profileSettings = profileSettingsMap[profileId]
                    dpt.conditions.forEach { condition ->
                        condition.changeMinMax(profileSettings?.minValue, profileSettings?.maxValue)
                        condition.update(
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
                }
            }

            // 3. 추가: 요청에 새롭게 포함된 프로필
            for (profileId in requestedProfileIds) {
                if (!existingProfileIds.contains(profileId)) {

                    val profile = DeviceProfileEnum.toEntity(profileId)

                    val dpt = DeviceProfileType(deviceProfile = profile, deviceType = deviceType)
                    em.persist(dpt)
                    em.flush()

                    val profileSettings: DeviceProfileTypeRequest? = profileSettingsMap[profileId]

                    deviceType.deviceEvents.forEach { event: DeviceEvent ->
                        val condition =
                            EventCondition(
                                deviceEvent = event,
                                operator =
                                    if (profile.fieldType ===
                                        DeviceProfile.FieldType.Boolean
                                    ) {
                                        EventCondition.ConditionOperator.EQUALS
                                    } else {
                                        EventCondition.ConditionOperator.BETWEEN
                                    },
                                value = "",
                                notificationEnabled = false,
                                locationTrackingEnabled = false,
                                soundEnabled = false,
                                fireEffectEnabled = false,
                                minValue = profileSettings?.minValue,
                                maxValue = profileSettings?.maxValue,
                            )
                        dpt.addCondition(condition)
                    }
                }
            }
        }

        updateSensorDataHandlerCache(deviceType)
        val savedDeviceType = deviceTypeRepository.save(deviceType)

        em.flush()
        em.clear()

        val refreshedDeviceType = findById(savedDeviceType.id!!)
        updateSensorDataHandlerCache(refreshedDeviceType)
    }

    private fun createOrUpdateEvents(eventRequests: List<DeviceEventRequest>): List<DeviceEvent> {
        val events = mutableListOf<DeviceEvent>()
        for (eventRequest in eventRequests) {
            var event: DeviceEvent
            if (eventRequest.id != null) {
                // 기존 이벤트 업데이트
                event = deviceEventRepository.findByIdOrNull(eventRequest.id) ?: throw CustomException(
                    ErrorCode.NOT_FOUND_DEVICE_EVENT,
                    eventRequest.id,
                )
                event.update(eventRequest.name, eventRequest.deviceLevel)

                if (eventRequest.iconId != event.iconId) {
                    eventRequest.iconId?.let { iconFileId ->
                        val iconFile = fileService.finalizeUpload(iconFileId, "${PREFIX}${event.id}/")
                        event.updateIconId(iconFile.id)
                    } ?: event.updateIconId(null)
                }
            } else {
                // 새 이벤트 생성 (DeviceType에 추가되기 전이므로 아직 저장하지 않음)
                event =
                    DeviceEvent(
                        name = eventRequest.name,
                        deviceLevel = eventRequest.deviceLevel,
                        iconId = eventRequest.iconId,
                    )
            }
            events.add(event)
        }
        return events
    }

    private fun updateSensorDataHandlerCache(deviceType: DeviceType) {
        val fullDeviceType = findById(deviceType.id!!)
        sensorDataHandler.updateDeviceTypeCache(fullDeviceType)
    }

    private fun removeSensorDataHandlerCache(deviceType: DeviceType) {
        sensorDataHandler.removeDeviceTypeFromCache(deviceType.objectId)
    }
}
