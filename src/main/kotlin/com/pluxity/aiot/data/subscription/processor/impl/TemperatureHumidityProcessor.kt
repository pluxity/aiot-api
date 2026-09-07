package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.TemperatureHumidity
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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class TemperatureHumidityProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val featureRepository: FeatureRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val TEMPERATURE = "Temperature"
        const val HUMIDITY = "Humidity"
        const val DISCOMFORT_INDEX = "DiscomfortIndex"
    }

    override fun getObjectId(): String = SensorType.TEMPERATURE_HUMIDITY.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        processEventConditions(
            deviceId = deviceId,
            sensorType = sensorType,
            values =
                buildList {
                    data.temperature?.let { add(TEMPERATURE to IncomingValue.Numeric(it)) }
                    data.humidity?.let { add(HUMIDITY to IncomingValue.Numeric(it)) }
                    // 온도와 습도가 모두 존재하면 불쾌 지수 계산
                    if (data.temperature != null && data.humidity != null) {
                        add(
                            DISCOMFORT_INDEX to
                                IncomingValue.Numeric(calculateDiscomfortIndex(data.temperature, data.humidity)),
                        )
                    }
                },
            timestamp = data.timestamp,
            messageSender = messageSender,
            eventHistoryRepository = eventHistoryRepository,
            featureRepository = featureRepository,
            eventConditionRepository = eventConditionRepository,
            eventPublisher = eventPublisher,
        )
        insertSensorData(data, siteId, deviceId, data.timestamp)
    }

    override fun insertSensorData(
        content: SubscriptionConResponse,
        siteId: Long,
        deviceId: String,
        timestamp: String,
    ) {
        content.temperature?.let {
            val tempMeasure =
                TemperatureHumidity(
                    siteId.toString(),
                    deviceId,
                    it,
                    "Temperature",
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, tempMeasure)
        }
        content.humidity?.let {
            val humidityMeasure =
                TemperatureHumidity(
                    siteId.toString(),
                    deviceId,
                    it,
                    "Humidity",
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, humidityMeasure)
        }
        if (content.temperature != null && content.humidity != null) {
            val discomfortMeasure =
                TemperatureHumidity(
                    siteId.toString(),
                    deviceId,
                    calculateDiscomfortIndex(content.temperature, content.humidity),
                    "DiscomfortIndex",
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, discomfortMeasure)
        }
    }

    // 불쾌 지수 계산
    private fun calculateDiscomfortIndex(
        temperature: Double,
        humidity: Double,
    ): Double = 0.81 * temperature + 0.01 * humidity * (0.99 * temperature - 14.3) + 46.3
}
