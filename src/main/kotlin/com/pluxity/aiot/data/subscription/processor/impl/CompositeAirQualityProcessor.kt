package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.CompositeAirQuality
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.data.subscription.processor.IncomingValue
import com.pluxity.aiot.data.subscription.processor.SensorDataProcessor
import com.pluxity.aiot.data.subscription.processor.toEventHistoryValue
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
class CompositeAirQualityProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val featureRepository: FeatureRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val TEMPERATURE = "Temperature"
        const val HUMIDITY = "Humidity"
        const val PM2_5 = "PM2.5"
        const val PM10 = "PM10"
        const val WIND_SPEED = "WindSpeed"
        const val WIND_DIRECTION = "WindDirection"
        const val UVI = "UVI"
        const val LED_LIGHT = "LED Light"
    }

    override fun getObjectId(): String = SensorType.COMPOSITE_AIR_QUALITY.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        val values = incomingValues(data)
        processEventConditions(
            deviceId = deviceId,
            sensorType = sensorType,
            values = values,
            timestamp = data.timestamp,
            messageSender = messageSender,
            eventHistoryRepository = eventHistoryRepository,
            featureRepository = featureRepository,
            eventConditionRepository = eventConditionRepository,
        )
        log.debug { "$values processed" }
        log.info {
            "${SensorType.COMPOSITE_AIR_QUALITY.description} - DeviceId: $deviceId, " +
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
        val time = DateTimeUtils.parseUtc(timestamp)
        incomingValues(content).forEach { (fieldKey, value) ->
            writeApi.writeMeasurement(
                WritePrecision.S,
                CompositeAirQuality(siteId.toString(), deviceId, fieldKey, value.toEventHistoryValue(), time),
            )
        }
    }

    /**
     * 수신된 데이터 중 값이 존재하는 계측 항목만 (fieldKey, 값) 쌍으로 변환한다.
     */
    private fun incomingValues(data: SubscriptionConResponse): List<Pair<String, IncomingValue>> =
        buildList {
            data.temperature?.let { add(TEMPERATURE to IncomingValue.Numeric(it)) }
            data.humidity?.let { add(HUMIDITY to IncomingValue.Numeric(it)) }
            data.pm25?.let { add(PM2_5 to IncomingValue.Numeric(it.toDouble())) }
            data.pm10?.let { add(PM10 to IncomingValue.Numeric(it.toDouble())) }
            data.windSpeed?.let { add(WIND_SPEED to IncomingValue.Numeric(it.toDouble())) }
            data.windDirection?.let { add(WIND_DIRECTION to IncomingValue.Numeric(it.toDouble())) }
            data.uvi?.let { add(UVI to IncomingValue.Numeric(it.toDouble())) }
            data.ledLight?.let { add(LED_LIGHT to IncomingValue.Numeric(it.toDouble())) }
        }
}
