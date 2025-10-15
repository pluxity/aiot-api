package com.pluxity.aiot.alarm.service.processor

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition

/**
 * 센서 데이터 Processor 테스트를 위한 공통 헬퍼 클래스
 */
abstract class ProcessorTestHelper(
    val deviceTypeRepository: DeviceTypeRepository,
    protected val deviceProfileRepository: DeviceProfileRepository,
    val siteRepository: SiteRepository,
    val featureRepository: FeatureRepository,
    protected val eventHistoryRepository: EventHistoryRepository,
    protected val actionHistoryService: ActionHistoryService,
    protected val sseServiceMock: SseService,
    protected val writeApiMock: WriteApi,
) {
    /**
     * DeviceProfile 캐시 (fieldKey 기반)
     */
    private val profileCache = mutableMapOf<String, DeviceProfile>()

    /**
     * DeviceProfile 조회 또는 생성 (캐싱)
     */
    fun getOrCreateProfile(
        fieldKey: String,
        description: String,
        fieldUnit: String,
        fieldType: DeviceProfile.FieldType,
    ): DeviceProfile =
        profileCache.getOrPut(fieldKey) {
            deviceProfileRepository.findAll().firstOrNull { it.fieldKey == fieldKey }
                ?: deviceProfileRepository.save(
                    DeviceProfile(
                        fieldKey = fieldKey,
                        description = description,
                        fieldUnit = fieldUnit,
                        fieldType = fieldType,
                    ),
                )
        }

    /**
     * 테스트용 DeviceType + EventCondition 생성
     */
    fun setupDeviceWithCondition(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        eventName: String,
        eventLevel: DeviceEvent.DeviceLevel,
        minValue: Double? = null,
        maxValue: Double? = null,
        operator: EventCondition.ConditionOperator,
        controlType: EventCondition.ControlType,
        guideMessage: String? = null,
        notificationIntervalMinutes: Int = 5,
        conditionValue: String? = null,
    ): TestSetup {
        // 1. DeviceType 생성
        val deviceType =
            DeviceType(
                objectId = objectId,
                description = "$objectId 설명",
                version = "1.0",
            )

        // 2. DeviceProfileType 생성 및 연결
        val deviceProfileType =
            DeviceProfileType(
                deviceProfile = profile,
                deviceType = deviceType,
            )
        deviceType.deviceProfileTypes.add(deviceProfileType)

        // 3. DeviceEvent 생성
        val deviceEvent =
            DeviceEvent(
                name = eventName,
                deviceLevel = eventLevel,
            )
        deviceEvent.updateDeviceType(deviceType)

        // 4. EventCondition 생성 및 연결
        val finalConditionValue =
            conditionValue ?: when (operator) {
                EventCondition.ConditionOperator.BETWEEN -> {
                    requireNotNull(minValue) { "BETWEEN operator requires minValue" }
                    requireNotNull(maxValue) { "BETWEEN operator requires maxValue" }
                    "$minValue,$maxValue"
                }
                EventCondition.ConditionOperator.EQUALS -> minValue?.toString() ?: "true"
                else -> minValue?.toString() ?: "0"
            }

        val condition =
            EventCondition(
                deviceEvent = deviceEvent,
                value = finalConditionValue,
                operator = operator,
                notificationEnabled = true,
                controlType = controlType,
                guideMessage = guideMessage,
                notificationIntervalMinutes = notificationIntervalMinutes,
                order = 1,
            )

        if (operator == EventCondition.ConditionOperator.BETWEEN) {
            condition.changeMinMax(minValue, maxValue)
        }

        deviceProfileType.addCondition(condition)

        // 6. DeviceType 저장 (CASCADE)
        val savedDeviceType = deviceTypeRepository.save(deviceType)

        // 7. Site & Feature 생성
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                deviceType = savedDeviceType,
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    /**
     * 더미 센서 데이터 생성
     */
    fun createSensorData(
        temperature: Double? = null,
        humidity: Double? = null,
        fireAlarm: Boolean? = null,
        angleX: Double? = null,
        angleY: Double? = null,
        timestamp: String = "20250115T103000",
    ): SubscriptionConResponse =
        SubscriptionConResponse(
            temperature = temperature,
            humidity = humidity,
            timestamp = timestamp,
            period = 60,
            fireAlarm = fireAlarm,
            angleX = angleX,
            angleY = angleY,
        )

    /**
     * notificationEnabled = false로 설정된 DeviceType 생성
     */
    fun setupDeviceWithDisabledEvent(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        eventName: String,
        eventLevel: DeviceEvent.DeviceLevel,
        minValue: Double?,
        maxValue: Double?,
        operator: EventCondition.ConditionOperator,
    ): TestSetup {
        val deviceType = DeviceType(objectId = objectId, description = "$objectId 설명", version = "1.0")
        val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = deviceType)
        deviceType.deviceProfileTypes.add(deviceProfileType)

        val deviceEvent = DeviceEvent(name = eventName, deviceLevel = eventLevel)
        deviceEvent.updateDeviceType(deviceType)

        val conditionValue =
            when (operator) {
                EventCondition.ConditionOperator.BETWEEN -> "$minValue,$maxValue"
                EventCondition.ConditionOperator.EQUALS -> minValue?.toString() ?: "true"
                else -> minValue?.toString() ?: "0"
            }

        val condition =
            EventCondition(
                deviceEvent = deviceEvent,
                value = conditionValue,
                operator = operator,
                notificationEnabled = false, // Disabled
                controlType = EventCondition.ControlType.MANUAL,
                guideMessage = "테스트",
                order = 1,
            )

        if (operator == EventCondition.ConditionOperator.BETWEEN) {
            condition.changeMinMax(minValue, maxValue)
        }

        deviceProfileType.addCondition(condition)

        val savedDeviceType = deviceTypeRepository.save(deviceType)
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                deviceType = savedDeviceType,
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    /**
     * 여러 EventCondition을 가진 복합 DeviceType 생성
     */
    fun setupDeviceWithMultipleConditions(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        conditions: List<ConditionSpec>,
    ): TestSetup {
        val deviceType = DeviceType(objectId = objectId, description = "$objectId 설명", version = "1.0")
        val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = deviceType)
        deviceType.deviceProfileTypes.add(deviceProfileType)

        conditions.forEachIndexed { index, spec ->
            val deviceEvent = DeviceEvent(name = spec.eventName, deviceLevel = spec.eventLevel)
            deviceEvent.updateDeviceType(deviceType)

            val conditionValue =
                when (spec.operator) {
                    EventCondition.ConditionOperator.BETWEEN -> "${spec.minValue},${spec.maxValue}"
                    EventCondition.ConditionOperator.EQUALS -> spec.minValue?.toString() ?: "true"
                    else -> spec.minValue?.toString() ?: "0"
                }

            val condition =
                EventCondition(
                    deviceEvent = deviceEvent,
                    value = conditionValue,
                    operator = spec.operator,
                    notificationEnabled = spec.notificationEnabled,
                    controlType = spec.controlType,
                    guideMessage = spec.guideMessage,
                    notificationIntervalMinutes = spec.notificationIntervalMinutes,
                    order = index + 1,
                )

            if (spec.operator == EventCondition.ConditionOperator.BETWEEN) {
                condition.changeMinMax(spec.minValue, spec.maxValue)
            }

            deviceProfileType.addCondition(condition)
        }

        val savedDeviceType = deviceTypeRepository.save(deviceType)
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                deviceType = savedDeviceType,
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    data class ConditionSpec(
        val eventName: String,
        val eventLevel: DeviceEvent.DeviceLevel,
        val minValue: Double?,
        val maxValue: Double?,
        val operator: EventCondition.ConditionOperator,
        val controlType: EventCondition.ControlType,
        val guideMessage: String? = null,
        val notificationIntervalMinutes: Int = 5,
        val notificationEnabled: Boolean = true,
    )

    data class TestSetup(
        val deviceType: DeviceType,
        val siteId: Long,
    )
}
