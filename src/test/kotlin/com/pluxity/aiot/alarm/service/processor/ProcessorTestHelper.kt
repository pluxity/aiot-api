package com.pluxity.aiot.alarm.service.processor

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import com.pluxity.aiot.system.event.condition.Operator

/**
 * 기존 파라미터를 새로운 구조로 변환하는 헬퍼 데이터 클래스
 */
data class ConvertedConditionParams(
    val dataType: DataType,
    val operator: Operator,
    val numericValue1: Double?,
    val numericValue2: Double?,
    val booleanValue: Boolean?,
)

/**
 * 기존 isBoolean, minValue, maxValue를 새로운 구조로 변환
 */
fun convertLegacyConditionParams(
    isBoolean: Boolean,
    minValue: String?,
    maxValue: String?,
): ConvertedConditionParams =
    if (isBoolean) {
        // Boolean 타입의 경우
        val boolValue = minValue?.toBoolean() ?: true
        ConvertedConditionParams(
            dataType = DataType.BOOLEAN,
            operator = Operator.EQUAL,
            numericValue1 = null,
            numericValue2 = null,
            booleanValue = boolValue,
        )
    } else {
        // Numeric 타입의 경우
        val min = minValue?.toDoubleOrNull()
        val max = maxValue?.toDoubleOrNull()

        when {
            min != null && max != null -> {
                // 두 값이 모두 있으면 BETWEEN
                ConvertedConditionParams(
                    dataType = DataType.NUMERIC,
                    operator = Operator.BETWEEN,
                    numericValue1 = min,
                    numericValue2 = max,
                    booleanValue = null,
                )
            }
            min != null -> {
                // minValue만 있으면 GREATER_OR_EQUAL
                ConvertedConditionParams(
                    dataType = DataType.NUMERIC,
                    operator = Operator.GREATER_OR_EQUAL,
                    numericValue1 = min,
                    numericValue2 = null,
                    booleanValue = null,
                )
            }
            max != null -> {
                // maxValue만 있으면 LESS_OR_EQUAL
                ConvertedConditionParams(
                    dataType = DataType.NUMERIC,
                    operator = Operator.LESS_OR_EQUAL,
                    numericValue1 = max,
                    numericValue2 = null,
                    booleanValue = null,
                )
            }
            else -> {
                // 기본값
                ConvertedConditionParams(
                    dataType = DataType.NUMERIC,
                    operator = Operator.GREATER_OR_EQUAL,
                    numericValue1 = 0.0,
                    numericValue2 = null,
                    booleanValue = null,
                )
            }
        }
    }

/**
 * EventCondition.ConditionLevel을 ConditionLevel로 변환
 */
fun mapDeviceEventLevelToConditionLevel(eventLevel: ConditionLevel): ConditionLevel =
    when (eventLevel) {
        ConditionLevel.NORMAL -> ConditionLevel.NORMAL
        ConditionLevel.WARNING -> ConditionLevel.WARNING
        ConditionLevel.CAUTION -> ConditionLevel.CAUTION
        ConditionLevel.DANGER -> ConditionLevel.DANGER
        ConditionLevel.DISCONNECTED -> ConditionLevel.DISCONNECTED
    }

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
    protected val eventConditionRepository: EventConditionRepository,
    protected val messageSenderMock: StompMessageSender,
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
        eventLevel: ConditionLevel,
        minValue: String? = null,
        maxValue: String? = null,
        needControl: Boolean = false,
        isBoolean: Boolean = false,
        notificationIntervalMinutes: Int = 0,
    ): TestSetup {
        // 1. DeviceType 조회 또는 생성 (objectId가 UNIQUE이므로 재사용)
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType =
                        DeviceType(
                            objectId = objectId,
                            description = "$objectId 설명",
                            version = "1.0",
                        )
                    val deviceProfileType =
                        DeviceProfileType(
                            deviceProfile = profile,
                            deviceType = newDeviceType,
                        )
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        // 2. 기존 EventCondition 삭제 (테스트 격리 - 같은 objectId로 중복 생성 방지)
        eventConditionRepository.deleteAllByObjectId(objectId)

        // 3. EventCondition 생성 및 저장
        val (dataType, operator, numericValue1, numericValue2, booleanValue) = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                isActivate = true,
                needControl = needControl,
                level = level,
                dataType = dataType,
                operator = operator,
                numericValue1 = numericValue1,
                numericValue2 = numericValue2,
                booleanValue = booleanValue,
                notificationEnabled = true,
                notificationIntervalMinutes = notificationIntervalMinutes,
                order = 1,
            )
        eventConditionRepository.save(condition)

        // 3. DeviceType는 이미 저장되어 있음
        val savedDeviceType = deviceType

        // 7. Site & Feature 생성
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
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
        eventLevel: ConditionLevel,
        minValue: String?,
        maxValue: String?,
        isBoolean: Boolean = false,
    ): TestSetup {
        // DeviceType 조회 또는 생성 (objectId가 UNIQUE이므로 재사용)
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType = DeviceType(objectId = objectId, description = "$objectId 설명", version = "1.0")
                    val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = newDeviceType)
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        // 기존 EventCondition 삭제 (테스트 격리)
        eventConditionRepository.deleteAllByObjectId(objectId)

        val (dataType, operator, numericValue1, numericValue2, booleanValue) = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                isActivate = true,
                needControl = true,
                level = level,
                dataType = dataType,
                operator = operator,
                numericValue1 = numericValue1,
                numericValue2 = numericValue2,
                booleanValue = booleanValue,
                notificationEnabled = false, // Disabled
                notificationIntervalMinutes = 0,
                order = 1,
            )
        eventConditionRepository.save(condition)

        val savedDeviceType = deviceType
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
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
        // DeviceType 조회 또는 생성 (objectId가 UNIQUE이므로 재사용)
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType = DeviceType(objectId = objectId, description = "$objectId 설명", version = "1.0")
                    val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = newDeviceType)
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        // 기존 EventCondition 삭제 (테스트 격리)
        eventConditionRepository.deleteAllByObjectId(objectId)

        conditions.forEachIndexed { index, spec ->
            val (dataType, operator, numericValue1, numericValue2, booleanValue) =
                convertLegacyConditionParams(
                    spec.isBoolean,
                    spec.minValue,
                    spec.maxValue,
                )
            val level = mapDeviceEventLevelToConditionLevel(spec.eventLevel)

            val condition =
                EventCondition(
                    objectId = objectId,
                    isActivate = true,
                    needControl = spec.needControl,
                    level = level,
                    dataType = dataType,
                    operator = operator,
                    numericValue1 = numericValue1,
                    numericValue2 = numericValue2,
                    booleanValue = booleanValue,
                    notificationEnabled = spec.notificationEnabled,
                    notificationIntervalMinutes = spec.notificationIntervalMinutes,
                    order = index + 1,
                )
            eventConditionRepository.save(condition)
        }

        val savedDeviceType = deviceType
        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    data class ConditionSpec(
        val eventName: String,
        val eventLevel: ConditionLevel,
        val minValue: String?,
        val maxValue: String?,
        val needControl: Boolean = false,
        val isBoolean: Boolean = false,
        val notificationIntervalMinutes: Int = 5,
        val notificationEnabled: Boolean = true,
    )

    data class TestSetup(
        val deviceType: DeviceType,
        val siteId: Long,
    )
}
