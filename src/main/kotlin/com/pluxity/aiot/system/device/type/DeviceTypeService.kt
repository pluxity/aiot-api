package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.alarm.service.SensorDataHandler
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
import com.pluxity.aiot.system.event.setting.EventSetting
import com.pluxity.aiot.system.event.setting.EventSettingHistoryRepository
import com.pluxity.aiot.system.event.setting.EventSettingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class DeviceTypeService(
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val eventSettingRepository: EventSettingRepository,
    private val deviceEventRepository: DeviceEventRepository,
    private val eventSettingHistoryRepository: EventSettingHistoryRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val sensorDataHandler: SensorDataHandler,
    private val em: EntityManager,
    private val featureRepository: FeatureRepository,
    private val fileService: FileService,
) {
    private val prefix = "device_events/"

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
    fun create(request: DeviceTypeRequest): Long {
        val deviceType =
            DeviceType(
                objectId = request.objectId,
                description = request.description,
                version = request.version,
            )

        // 이벤트 생성 및 연결
        val newEventIconIds = mutableMapOf<DeviceEvent, Long>() // 새 이벤트의 iconId 저장
        if (request.deviceEvents != null && request.deviceEvents.isNotEmpty()) {
            val eventsList = createOrUpdateEvents(request.deviceEvents)
            eventsList.forEach { event ->
                event.updateDeviceType(deviceType)
                // 새 이벤트의 iconId를 임시 저장 (id가 없으면 새 이벤트)
                if (event.id == null && event.iconId != null) {
                    newEventIconIds[event] = event.iconId!!
                }
            }
        }

        // 모든 연관관계 설정 후 한 번에 저장
        val savedDeviceType = deviceTypeRepository.save(deviceType)

        // 새로 생성된 DeviceEvent의 iconId 처리
        newEventIconIds.forEach { (event, iconId) ->
            val iconFile = fileService.finalizeUpload(iconId, "${prefix}${event.id}/")
            event.updateIconId(iconFile.id)
        }

        // 프로필 연결 및 이벤트 세팅 설정
        if (request.deviceProfileTypes != null && request.deviceProfileTypes.isNotEmpty()) {
            // 각 프로필 설정의 최소값/최대값 유효성 검사
            request.deviceProfileTypes
                .forEach { profileSettings: DeviceProfileTypeRequest -> this.validateMinMaxValues(profileSettings) }

            val profileIds =
                request.deviceProfileTypes
                    .map { it.profileId }
                    .distinct()

            val profiles = deviceProfileRepository.findAllById(profileIds)
            if (profiles.size != profileIds.size) {
                throw CustomException(ErrorCode.NOT_FOUND_DEVICE_PROFILE)
            }

            val profileSettingsMap: Map<Long, DeviceProfileTypeRequest> =
                request.deviceProfileTypes
                    .groupBy { it.profileId }
                    .mapValues { it.value.first() }

            // 프로필 연결 및 EventSetting 생성
            profiles.forEach { profile: DeviceProfile ->
                val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = savedDeviceType)
                savedDeviceType.deviceProfileTypes.add(deviceProfileType)
                val profileSettings: DeviceProfileTypeRequest? = profileSettingsMap[profile.id]
                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = false,
                        isOriginal = true,
                    )

                savedDeviceType.deviceEvents.forEach { event: DeviceEvent ->
                    val condition =
                        EventCondition(
                            deviceEvent = event,
                            operator =
                                if (profile.fieldType ==
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
                    eventSetting.addCondition(condition)
                }
                deviceProfileType.addEventSetting(eventSetting)
            }
        }

        updateSensorDataHandlerCache(savedDeviceType)
        return savedDeviceType.id!!
    }

    @Transactional
    fun update(
        id: Long,
        request: DeviceTypeRequest,
    ) {
        log.info { "DeviceType 업데이트 시작 - ID: $id" }

        val deviceType = findById(id)

        val oldObjectId = deviceType.objectId
        val newObjectId = request.objectId
        val objectIdChanged = oldObjectId != newObjectId

        deviceType.update(request.objectId, request.description, request.version)

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
            val newEventIconIds = events
                .filter { it.id == null && it.iconId != null }
                .associateWith { it.iconId!! }

            deviceType.updateDeviceEvents(events)

            // 변경 내용 저장 (새 DeviceEvent에 id가 생성됨)
            em.flush()

            // 새로 생성된 DeviceEvent의 iconId 처리
            newEventIconIds.forEach { (event, iconId) ->
                val iconFile = fileService.finalizeUpload(iconId, "${prefix}${event.id}/")
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
                val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(dpt.id!!)
                eventSettings.forEach { eventSetting: EventSetting ->
                    // 먼저 이력 삭제
                    eventSettingHistoryRepository.deleteAllByEventSettingId(eventSetting.id!!)
                }
                // 그 다음 이벤트 설정 삭제
                eventSettingRepository.deleteAll(eventSettings)
                deviceType.removeDeviceProfile(dpt.deviceProfile)
            }

            // 2. 업데이트: 기존 연관 프로필의 EventSetting 조건 업데이트
            for (dpt in deviceType.deviceProfileTypes) {
                val profileId = dpt.deviceProfile?.id
                if (requestedProfileIds.contains(profileId)) {
                    val profileSettings = profileSettingsMap[profileId]
                    val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(dpt.id!!)
                    eventSettings.forEach { eventSetting: EventSetting ->
                        eventSetting.conditions.forEach { condition ->
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
            }

            // 3. 추가: 요청에 새롭게 포함된 프로필
            for (profileId in requestedProfileIds) {
                if (!existingProfileIds.contains(profileId)) {
                    val profile =
                        deviceProfileRepository.findByIdOrNull(id)
                            ?: throw CustomException(ErrorCode.NOT_FOUND_DEVICE_PROFILE, id)

                    val dpt = DeviceProfileType(deviceProfile = profile, deviceType = deviceType)
                    em.persist(dpt)
                    em.flush()

                    val profileSettings: DeviceProfileTypeRequest? = profileSettingsMap[profileId]

                    val eventSetting =
                        EventSetting(
                            deviceProfileType = dpt,
                            eventEnabled = false,
                            isOriginal = true,
                        )

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
                        eventSetting.addCondition(condition)
                    }

                    dpt.addEventSetting(eventSetting)
                    eventSettingRepository.save(eventSetting)
                }
            }
        }

        // objectId가 변경된 경우 Feature 연관관계 재설정
        if (objectIdChanged) {
            log.info { "DeviceType objectId 변경됨: $oldObjectId -> $newObjectId, Feature 연관관계 재설정 시작" }

            // 1. 현재 이 DeviceType과 연결된 모든 Feature 가져오기
            val connectedFeatures = deviceType.features

            // 2. ObjectId가 현재 DeviceType의 objectId와 일치하지 않는 Feature 분리
            for (feature in connectedFeatures) {
                val featureObjectId = feature.objectId
                if (!featureObjectId.contains(newObjectId)) {
                    log.info {
                        "Feature(id:${feature.id}, deviceId:${feature.deviceId})의 objectId($featureObjectId)가 변경된 DeviceType objectId($newObjectId)와 일치하지 않아 연결 해제"
                    }

                    // Feature에서 이 DeviceType 연결 해제
                    deviceType.removeFeature(feature)
                }
            }

            // 3. ObjectId가 newObjectId와 일치하는 다른 Feature 찾아서 연결
            // FIXME: objectId가 하나 이상으로 가지고 있으면 위험 할 수 있다. 추후에 문제되면 아래 로직 변경필요
            val featuresToConnect = featureRepository.findByObjectIdContaining(newObjectId)
            for (feature in featuresToConnect) {
                if (feature.deviceType == null || feature.deviceType != deviceType) {
                    log.info {
                        "Feature(id:${feature.id}, deviceId:${feature.deviceId})의 objectId(${feature.objectId})가 변경된 DeviceType objectId($newObjectId)와 일치하여 연결"
                    }

                    // Feature에 현재 DeviceType 연결
                    deviceType.addFeature(feature)
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

    @Transactional
    fun delete(id: Long) {
        val deviceType = findById(id)
        deviceType.deviceProfileTypes.forEach { deviceProfileType: DeviceProfileType ->
            val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(deviceProfileType.id!!)
            eventSettings.forEach { eventSetting: EventSetting ->
                eventSettingHistoryRepository.deleteAllByEventSettingId(eventSetting.id!!)
                eventConditionRepository.deleteAllById(eventSetting.conditions.map { it.id })
            }
            eventSettingRepository.deleteAll(eventSettings)
        }
        deviceTypeRepository.delete(deviceType)
        removeSensorDataHandlerCache(deviceType)
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
                        val iconFile = fileService.finalizeUpload(iconFileId, "${prefix}${event.id}/")
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
