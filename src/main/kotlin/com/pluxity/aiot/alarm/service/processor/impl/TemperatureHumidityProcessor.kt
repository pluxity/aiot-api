package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.measure.TemperatureHumidity
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.system.device.type.DeviceType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class TemperatureHumidityProcessor(
    private val sseService: SseService,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryService: ActionHistoryService,
    private val featureRepository: FeatureRepository,
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
        deviceType: DeviceType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        data.temperature?.let {
            processEventConditions(
                deviceId = deviceId,
                deviceType = deviceType,
                fieldKey = TEMPERATURE,
                value = data.temperature,
                timestamp = data.timestamp,
                sseService = sseService,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
            )
            log.debug { "Temperature processed: $it°C" }
        }

        data.humidity?.let {
            processEventConditions(
                deviceId = deviceId,
                deviceType = deviceType,
                fieldKey = HUMIDITY,
                value = data.humidity,
                timestamp = data.timestamp,
                sseService = sseService,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
            )
            log.debug { "Humidity processed: $it%" }
        }

        // 온도와 습도가 모두 존재하면 불쾌 지수 계산
        if (data.temperature != null && data.humidity != null) {
            // 불쾌 지수 처리
            processEventConditions(
                deviceId,
                deviceType,
                DISCOMFORT_INDEX,
                calculateDiscomfortIndex(data.temperature, data.humidity),
                data.timestamp,
                sseService,
                eventHistoryRepository,
                actionHistoryService,
                featureRepository,
            )
            log.debug {
                "Discomfort Index calculated: ${"%.2f".format(calculateDiscomfortIndex(data.temperature, data.humidity))} " +
                    "(Temp: ${data.temperature}°C, Humidity: ${data.humidity}%)"
            }
        }
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
