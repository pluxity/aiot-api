package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.measure.FireAlarm
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.system.device.type.DeviceType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class FireAlarmProcessor(
    private val sseService: SseService,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryService: ActionHistoryService,
    private val featureRepository: FeatureRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val FIRE_ALARM = "Fire Alarm"
    }

    override fun getObjectId(): String = SensorType.FIRE.objectId

    override fun process(
        deviceId: String,
        deviceType: DeviceType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        data.fireAlarm?.let {
            processEventConditions(
                deviceId = deviceId,
                deviceType = deviceType,
                fieldKey = FIRE_ALARM,
                value = it, // Boolean 값 그대로 전달
                timestamp = data.timestamp,
                sseService = sseService,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
            )
            log.debug { "Fire Alarm processed: $it" }
        }
        insertSensorData(data, siteId, deviceId, data.timestamp)
    }

    override fun insertSensorData(
        content: SubscriptionConResponse,
        siteId: Long,
        deviceId: String,
        timestamp: String,
    ) {
        content.fireAlarm?.let {
            val fireAlarm =
                FireAlarm(
                    siteId.toString(),
                    deviceId,
                    if (it) 1.0 else 0.0,
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, fireAlarm)
        }
    }
}
