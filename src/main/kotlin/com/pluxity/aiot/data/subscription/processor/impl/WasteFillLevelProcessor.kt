package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.WasteFillLevel
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.data.subscription.processor.IncomingValue
import com.pluxity.aiot.data.subscription.processor.SensorDataProcessor
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class WasteFillLevelProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val featureRepository: FeatureRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val CONTAINER_MODULE_ID = "ContainerModuleId"
        const val ACTUAL_FILLING = "ActualFilling"
        const val HIGH_THRESHOLD = "HighThreshold"
    }

    override fun getObjectId(): String = SensorType.WASTE_FILL_LEVEL.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        // ContainerModuleId(식별 값)와 HighThreshold(단말이 보고하는 만재 기준값)는 적재만 하고
        // 이벤트 판정은 시스템에 등록된 EventCondition 기준으로만 수행한다

        data.actualFilling?.let {
            processEventConditions(
                deviceId = deviceId,
                sensorType = sensorType,
                fieldKey = ACTUAL_FILLING,
                value = IncomingValue.Numeric(it.toDouble()),
                timestamp = data.timestamp,
                messageSender = messageSender,
                eventHistoryRepository = eventHistoryRepository,
                featureRepository = featureRepository,
                eventConditionRepository = eventConditionRepository,
            )
            log.debug { "ActualFilling value: $it" }
        }
        log.info {
            "${SensorType.WASTE_FILL_LEVEL.description} - DeviceId: $deviceId, " +
                "Timestamp: ${data.timestamp}, Period: ${data.period}"
        }
        insertSensorData(data, siteId, deviceId, data.timestamp)
    }

    override fun insertSensorData(
        content: SubscriptionConResponse,
        siteId: Long,
        deviceId: String,
        timestamp: String,
    ) {
        content.containerModuleId?.let {
            val wasteFillLevel =
                WasteFillLevel(
                    siteId.toString(),
                    deviceId,
                    CONTAINER_MODULE_ID,
                    it.toDouble(),
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, wasteFillLevel)
        }
        content.actualFilling?.let {
            val wasteFillLevel =
                WasteFillLevel(
                    siteId.toString(),
                    deviceId,
                    ACTUAL_FILLING,
                    it.toDouble(),
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, wasteFillLevel)
        }
        content.highThreshold?.let {
            val wasteFillLevel =
                WasteFillLevel(
                    siteId.toString(),
                    deviceId,
                    HIGH_THRESHOLD,
                    it.toDouble(),
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, wasteFillLevel)
        }
    }
}
