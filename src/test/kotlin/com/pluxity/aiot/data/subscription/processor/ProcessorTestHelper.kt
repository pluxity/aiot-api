package com.pluxity.aiot.data.subscription.processor

import com.influxdb.client.WriteApi
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import org.springframework.context.ApplicationEventPublisher

data class ConvertedConditionParams(
    val conditionType: ConditionType,
    val operator: Operator,
    val thresholdValue: Double?,
    val leftValue: Double?,
    val rightValue: Double?,
    val booleanValue: Boolean?,
)

fun convertLegacyConditionParams(
    isBoolean: Boolean,
    minValue: String?,
    maxValue: String?,
): ConvertedConditionParams =
    if (isBoolean) {
        val boolValue = minValue?.toBoolean() ?: true
        ConvertedConditionParams(
            conditionType = ConditionType.SINGLE,
            operator = Operator.GE,
            thresholdValue = null,
            leftValue = null,
            rightValue = null,
            booleanValue = boolValue,
        )
    } else {
        val min = minValue?.toDoubleOrNull()
        val max = maxValue?.toDoubleOrNull()

        when {
            min != null && max != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.RANGE,
                    operator = Operator.BETWEEN,
                    thresholdValue = null,
                    leftValue = min,
                    rightValue = max,
                    booleanValue = null,
                )
            }
            min != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.GE,
                    thresholdValue = min,
                    leftValue = null,
                    rightValue = null,
                    booleanValue = null,
                )
            }
            max != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.LE,
                    thresholdValue = max,
                    leftValue = null,
                    rightValue = null,
                    booleanValue = null,
                )
            }
            else -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.GE,
                    thresholdValue = 0.0,
                    leftValue = null,
                    rightValue = null,
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
    val siteRepository: SiteRepository,
    val featureRepository: FeatureRepository,
    protected val eventHistoryRepository: EventHistoryRepository,
    protected val eventConditionRepository: EventConditionRepository,
    protected val messageSenderMock: StompMessageSender,
    protected val writeApiMock: WriteApi,
) {
    /** 프로세서 테스트는 발행 여부를 보지 않으므로 무해한 대체품을 둔다 */
    protected val eventPublisherMock = ApplicationEventPublisher { _ -> }

    /**
     * 테스트용 DeviceType + EventCondition 생성
     */
    fun setupDeviceWithCondition(
        objectId: String,
        deviceId: String,
        eventLevel: ConditionLevel,
        minValue: String? = null,
        maxValue: String? = null,
        isBoolean: Boolean = false,
        fieldKey: String,
    ): TestSetup {
        val sensorType = SensorType.fromObjectId(objectId)

        // 2. 기존 EventCondition 삭제 (테스트 격리 - 같은 objectId로 중복 생성 방지)
        eventConditionRepository.deleteAllByObjectId(objectId)

        // 3. EventCondition 생성 및 저장
        val params = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                fieldKey = fieldKey,
                objectId = objectId,
                isActivate = true,
                level = level,
                conditionType = params.conditionType,
                operator = params.operator,
                thresholdValue = params.thresholdValue,
                leftValue = params.leftValue,
                rightValue = params.rightValue,
                booleanValue = params.booleanValue,
                notificationEnabled = true,
            )
        eventConditionRepository.save(condition)

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

        return TestSetup(sensorType, site.requiredId)
    }

    /**
     * 더미 센서 데이터 생성
     */
    fun createSensorData(
        temperature: Double? = null,
        humidity: Double? = null,
        fireAlarm: Boolean? = null,
        containerModuleId: Int? = null,
        actualFilling: Int? = null,
        highThreshold: Int? = null,
        fireDetection: Boolean? = null,
        co2: Int? = null,
        co: Int? = null,
        tvoc: Int? = null,
        fireCauseMask: Int? = null,
        nh3: Int? = null,
        h2s: Int? = null,
        numberOfVisitors: Int? = null,
        numberOfLeavers: Int? = null,
        pm25: Int? = null,
        pm10: Int? = null,
        windSpeed: Int? = null,
        windDirection: Int? = null,
        uvi: Int? = null,
        ledLight: Int? = null,
        timestamp: String = "20250115T103000",
    ): SubscriptionConResponse =
        SubscriptionConResponse(
            temperature = temperature,
            humidity = humidity,
            timestamp = timestamp,
            period = 60,
            fireAlarm = fireAlarm,
            containerModuleId = containerModuleId,
            actualFilling = actualFilling,
            highThreshold = highThreshold,
            fireDetection = fireDetection,
            co2 = co2,
            co = co,
            tvoc = tvoc,
            fireCauseMask = fireCauseMask,
            nh3 = nh3,
            h2s = h2s,
            numberOfVisitors = numberOfVisitors,
            numberOfLeavers = numberOfLeavers,
            pm25 = pm25,
            pm10 = pm10,
            windSpeed = windSpeed,
            windDirection = windDirection,
            uvi = uvi,
            ledLight = ledLight,
        )

    /**
     * notificationEnabled = false로 설정된 DeviceType 생성
     */
    fun setupDeviceWithDisabledEvent(
        objectId: String,
        deviceId: String,
        eventLevel: ConditionLevel,
        minValue: String?,
        maxValue: String?,
        isBoolean: Boolean = false,
    ): TestSetup {
        val sensorType = SensorType.fromObjectId(objectId)

        // 기존 EventCondition 삭제 (테스트 격리)
        eventConditionRepository.deleteAllByObjectId(objectId)

        val params = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                fieldKey = sensorType.deviceProfiles.first().fieldKey,
                objectId = objectId,
                isActivate = false,
                level = level,
                conditionType = params.conditionType,
                operator = params.operator,
                thresholdValue = params.thresholdValue,
                leftValue = params.leftValue,
                rightValue = params.rightValue,
                booleanValue = params.booleanValue,
                notificationEnabled = false,
            )
        eventConditionRepository.save(condition)

        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId Sensor",
                site = site,
            ),
        )

        return TestSetup(sensorType, site.requiredId)
    }

    /**
     * 여러 EventCondition을 가진 복합 DeviceType 생성
     */
    fun setupDeviceWithMultipleConditions(
        objectId: String,
        deviceId: String,
        conditions: List<ConditionSpec>,
    ): TestSetup {
        val sensorType = SensorType.fromObjectId(objectId)
        // 기존 EventCondition 삭제 (테스트 격리)
        eventConditionRepository.deleteAllByObjectId(objectId)

        conditions.forEachIndexed { index, spec ->
            val params =
                convertLegacyConditionParams(
                    spec.isBoolean,
                    spec.minValue,
                    spec.maxValue,
                )
            val level = mapDeviceEventLevelToConditionLevel(spec.eventLevel)

            val condition =
                EventCondition(
                    fieldKey = sensorType.deviceProfiles.first().fieldKey,
                    objectId = objectId,
                    isActivate = true,
                    level = level,
                    conditionType = params.conditionType,
                    operator = params.operator,
                    thresholdValue = params.thresholdValue,
                    leftValue = params.leftValue,
                    rightValue = params.rightValue,
                    booleanValue = params.booleanValue,
                    notificationEnabled = spec.notificationEnabled,
                )
            eventConditionRepository.save(condition)
        }

        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId Sensor",
                site = site,
            ),
        )

        return TestSetup(sensorType, site.requiredId)
    }

    data class ConditionSpec(
        val eventLevel: ConditionLevel,
        val minValue: String?,
        val maxValue: String?,
        val needControl: Boolean = false,
        val isBoolean: Boolean = false,
        val notificationIntervalMinutes: Int = 5,
        val notificationEnabled: Boolean = true,
    )

    data class TestSetup(
        val sensorType: SensorType,
        val siteId: Long,
    )
}
