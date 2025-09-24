package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.measure.DisplacementGauge
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.system.device.type.DeviceType
import io.github.oshai.kotlinlogging.KotlinLogging

private val log = KotlinLogging.logger {}

class DisplacementGaugeProcessor(
    private val sseService: SseService,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryService: ActionHistoryService,
    private val featureRepository: FeatureRepository,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val ANGLE_X = "AngleX"
        const val ANGLE_Y = "AngleY"
    }

    override fun getObjectId(): String = SensorType.DISPLACEMENT_GAUGE.objectId

    override fun process(
        deviceId: String,
        deviceType: DeviceType,
        facilityId: Long,
        data: SubscriptionConResponse,
    ) {
        data.angleX?.let {
            processEventConditions(
                deviceId = deviceId,
                deviceType = deviceType,
                fieldKey = ANGLE_X,
                value = it,
                timestamp = data.timestamp,
                sseService = sseService,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
            )
            log.debug { "Angle-X value: $it" }
        }
        data.angleY?.let {
            processEventConditions(
                deviceId = deviceId,
                deviceType = deviceType,
                fieldKey = ANGLE_Y,
                value = it,
                timestamp = data.timestamp,
                sseService = sseService,
                eventHistoryRepository = eventHistoryRepository,
                actionHistoryService = actionHistoryService,
                featureRepository = featureRepository,
            )
            log.debug { "Angle-Y value: $it" }
        }
        log.info {
            "${SensorType.DISPLACEMENT_GAUGE.description} - DeviceId: $deviceId, " +
                "Timestamp: ${data.timestamp}, Period: ${data.period}"
        }
        insertSensorData(data, facilityId, deviceId, data.timestamp)
    }

    override fun insertSensorData(
        content: SubscriptionConResponse,
        facilityId: Long,
        deviceId: String,
        timestamp: String,
    ) {
        content.angleX?.let {
            val displacementGauge =
                DisplacementGauge(
                    facilityId.toString(),
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
                    facilityId.toString(),
                    deviceId,
                    ANGLE_Y,
                    it,
                    DateTimeUtils.parseUtc(timestamp),
                )
            writeApi.writeMeasurement(WritePrecision.S, displacementGauge)
        }
    }
}
