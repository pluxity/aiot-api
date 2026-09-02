package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.PeopleCounter
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
class PeopleCounterProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val featureRepository: FeatureRepository,
    private val eventConditionRepository: EventConditionRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val NUMBER_OF_VISITORS = "NumberOfVisitors"
        const val NUMBER_OF_LEAVERS = "NumberOfLeavers"
    }

    override fun getObjectId(): String = SensorType.PEOPLE_COUNTER.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        incomingValues(data).forEach { (fieldKey, value) ->
            processEventConditions(
                deviceId = deviceId,
                sensorType = sensorType,
                fieldKey = fieldKey,
                value = value,
                timestamp = data.timestamp,
                messageSender = messageSender,
                eventHistoryRepository = eventHistoryRepository,
                featureRepository = featureRepository,
                eventConditionRepository = eventConditionRepository,
            )
            log.debug { "$fieldKey processed: $value" }
        }
        log.info {
            "${SensorType.PEOPLE_COUNTER.description} - DeviceId: $deviceId, " +
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
                PeopleCounter(siteId.toString(), deviceId, fieldKey, value.toEventHistoryValue(), time),
            )
        }
    }

    /**
     * 수신된 데이터 중 값이 존재하는 계측 항목만 (fieldKey, 값) 쌍으로 변환한다.
     */
    private fun incomingValues(data: SubscriptionConResponse): List<Pair<String, IncomingValue>> =
        buildList {
            data.numberOfVisitors?.let { add(NUMBER_OF_VISITORS to IncomingValue.Numeric(it.toDouble())) }
            data.numberOfLeavers?.let { add(NUMBER_OF_LEAVERS to IncomingValue.Numeric(it.toDouble())) }
        }
}
