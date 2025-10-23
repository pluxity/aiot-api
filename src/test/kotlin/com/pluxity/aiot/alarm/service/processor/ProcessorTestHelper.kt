package com.pluxity.aiot.alarm.service.processor

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
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
    val siteRepository: SiteRepository,
    val featureRepository: FeatureRepository,
    protected val eventHistoryRepository: EventHistoryRepository,
    protected val actionHistoryService: ActionHistoryService,
    protected val eventConditionRepository: EventConditionRepository,
    protected val messageSenderMock: StompMessageSender,
    protected val writeApiMock: WriteApi,
) {
    /**
     * 테스트용 DeviceType + EventCondition 생성
     */
    fun setupDeviceWithCondition(
        objectId: String,
        deviceId: String,
        eventName: String,
        eventLevel: ConditionLevel,
        minValue: String? = null,
        maxValue: String? = null,
        isBoolean: Boolean = false,
    ): TestSetup {
        val sensorType = SensorType.fromObjectId(objectId)

        // 2. 기존 EventCondition 삭제 (테스트 격리 - 같은 objectId로 중복 생성 방지)
        eventConditionRepository.deleteAllByObjectId(objectId)

        // 3. EventCondition 생성 및 저장
        val (dataType, operator, numericValue1, numericValue2, booleanValue) = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                isActivate = true,
                level = level,
                dataType = dataType,
                operator = operator,
                numericValue1 = numericValue1,
                numericValue2 = numericValue2,
                booleanValue = booleanValue,
                notificationEnabled = true,
                order = 1,
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

        return TestSetup(sensorType, site.id!!)
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
        eventName: String,
        eventLevel: ConditionLevel,
        minValue: String?,
        maxValue: String?,
        isBoolean: Boolean = false,
    ): TestSetup {
        val sensorType = SensorType.fromObjectId(objectId)

        // 기존 EventCondition 삭제 (테스트 격리)
        eventConditionRepository.deleteAllByObjectId(objectId)

        val (dataType, operator, numericValue1, numericValue2, booleanValue) = convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                isActivate = true,
                level = level,
                dataType = dataType,
                operator = operator,
                numericValue1 = numericValue1,
                numericValue2 = numericValue2,
                booleanValue = booleanValue,
                notificationEnabled = false, // Disabled
                order = 1,
            )
        eventConditionRepository.save(condition)

        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                site = site,
            ),
        )

        return TestSetup(sensorType, site.id!!)
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
                    level = level,
                    dataType = dataType,
                    operator = operator,
                    numericValue1 = numericValue1,
                    numericValue2 = numericValue2,
                    booleanValue = booleanValue,
                    notificationEnabled = spec.notificationEnabled,
                    order = index + 1,
                )
            eventConditionRepository.save(condition)
        }

        val site = siteRepository.save(SiteFixture.create(name = "테스트 현장 $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId 센서",
                site = site,
            ),
        )

        return TestSetup(sensorType, site.id!!)
    }

    data class ConditionSpec(
        val eventName: String,
        val eventLevel: ConditionLevel,
        val minValue: String?,
        val maxValue: String?,
        val isBoolean: Boolean = false,
        val notificationEnabled: Boolean = true,
    )

    data class TestSetup(
        val sensorType: SensorType,
        val siteId: Long,
    )
}
