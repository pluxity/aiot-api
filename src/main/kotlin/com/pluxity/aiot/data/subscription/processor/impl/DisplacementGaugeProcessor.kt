package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.data.measure.DisplacementGauge
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.data.subscription.processor.SensorDataProcessor
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.system.event.condition.ConditionType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import com.pluxity.aiot.system.event.condition.Operator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class DisplacementGaugeProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryService: ActionHistoryService,
    private val featureRepository: FeatureRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val ANGLE_X = "Angle-X"
        const val ANGLE_Y = "Angle-Y"
    }

    override fun getObjectId(): String = SensorType.DISPLACEMENT_GAUGE.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        data.angleX?.let {
            processEventConditions(
                deviceId = deviceId,
                sensorType = sensorType,
                fieldKey = ANGLE_X,
                value = it,
                timestamp = data.timestamp,
                messageSender = messageSender,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
                eventConditionRepository = eventConditionRepository,
            )
            log.debug { "Angle-X value: \$it" }
        }
        data.angleY?.let {
            processEventConditions(
                deviceId = deviceId,
                sensorType = sensorType,
                fieldKey = ANGLE_Y,
                value = it,
                timestamp = data.timestamp,
                messageSender = messageSender,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
                eventConditionRepository = eventConditionRepository,
            )
            log.debug { "Angle-Y value: \$it" }
        }
        log.info {
            "\${SensorType.DISPLACEMENT_GAUGE.description} - DeviceId: \$deviceId, " +
                "Timestamp: \${data.timestamp}, Period: \${data.period}"
        }
        insertSensorData(data, siteId, deviceId, data.timestamp)
    }

    override fun insertSensorData(
        content: SubscriptionConResponse,
        siteId: Long,
        deviceId: String,
        timestamp: String,
    ) {
        content.angleX?.let {
            val displacementGauge =
                DisplacementGauge(
                    siteId.toString(),
                    deviceId,
                    ANGLE_X,
                    it,
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, displacementGauge)
        }
        content.angleY?.let {
            val displacementGauge =
                DisplacementGauge(
                    siteId.toString(),
                    deviceId,
                    ANGLE_Y,
                    it,
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, displacementGauge)
        }
    }

    /**
     * 변위계 센서(AngleX, AngleY)의 특수한 BETWEEN 처리를 위한 override
     * - leftValue: 오차 범위 (errorRange)
     * - rightValue: 중앙값 (centerValue)
     * - 조건: 실제 값이 (중앙값 - 오차범위) ~ (중앙값 + 오차범위) 범위를 벗어나면 true
     */
    override fun isConditionMet(
        condition: EventCondition,
        value: Any,
        fieldKey: String,
    ): Boolean {
        // 각도계 센서(AngleX, AngleY)이고 BETWEEN 연산자인 경우 특수 처리
        if ((fieldKey == ANGLE_X || fieldKey == ANGLE_Y) &&
            value is Double &&
            condition.conditionType == ConditionType.RANGE &&
            condition.operator == Operator.BETWEEN &&
            condition.leftValue != null &&
            condition.rightValue != null
        ) {
            val errorRange = condition.leftValue!!
            val centerValue = condition.rightValue!!

            // 중앙값 ± 오차 범위 계산
            val minRange = centerValue - errorRange
            val maxRange = centerValue + errorRange

            // 실제 값이 (중앙값-오차) ~ (중앙값+오차) 범위 밖에 있는지 확인
            return value <= minRange || value >= maxRange
        }

        // 일반적인 처리는 부모 인터페이스의 기본 구현 사용
        return super.isConditionMet(condition, value, fieldKey)
    }
}
