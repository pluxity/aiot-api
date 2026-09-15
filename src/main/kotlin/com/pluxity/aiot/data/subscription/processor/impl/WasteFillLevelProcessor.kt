package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.WasteFillLevel
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.data.subscription.processor.EventTrigger
import com.pluxity.aiot.data.subscription.processor.SensorDataProcessor
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class WasteFillLevelProcessor(
    private val messageSender: StompMessageSender,
    private val eventHistoryRepository: EventHistoryRepository,
    private val featureRepository: FeatureRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val incidentService: IncidentService,
    private val writeApi: WriteApi,
) : SensorDataProcessor {
    companion object {
        const val CONTAINER_MODULE_ID = "ContainerModuleId"
        const val ACTUAL_FILLING = "ActualFilling"
        const val HIGH_THRESHOLD = "HighThreshold"
        const val FULL_GUIDE_MESSAGE = "쓰레기통이 가득 찼습니다. 수거가 필요합니다."
    }

    override fun getObjectId(): String = SensorType.WASTE_FILL_LEVEL.objectId

    override fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    ) {
        evaluateFullness(deviceId, sensorType, data)
        log.info {
            "${SensorType.WASTE_FILL_LEVEL.description} - DeviceId: $deviceId, " +
                "Timestamp: ${data.timestamp}, Period: ${data.period}"
        }
        insertSensorData(data, siteId, deviceId, data.timestamp)
    }

    /**
     * ActualFilling은 센서에서 적재면까지의 거리라 값이 작을수록 가득 찬 상태다.
     * 단말이 함께 보고하는 HighThreshold보다 작으면 만재로 보고 즉시 경고를 낸다.
     * 둘 중 하나라도 없으면 판단 근거가 없으므로 상태를 그대로 둔다.
     */
    private fun evaluateFullness(
        deviceId: String,
        sensorType: SensorType,
        data: SubscriptionConResponse,
    ) {
        val actualFilling = data.actualFilling?.toDouble() ?: return
        val highThreshold = data.highThreshold?.toDouble() ?: return
        val feature = getFeatureFromCacheOrDb(deviceId, featureRepository)

        if (actualFilling >= highThreshold) {
            updateFeatureEventStatus(feature, ConditionLevel.NORMAL.toString(), featureRepository)
            return
        }

        processEvent(
            deviceId = deviceId,
            sensorType = sensorType,
            fieldKey = ACTUAL_FILLING,
            value = actualFilling,
            fieldUnit = DeviceProfileEnum.ACTUAL_FILLING.unit,
            fieldDescription = DeviceProfileEnum.ACTUAL_FILLING.description,
            trigger =
                EventTrigger(
                    level = ConditionLevel.WARNING,
                    guideMessage = FULL_GUIDE_MESSAGE,
                    notificationEnabled = true,
                    minValue = highThreshold,
                    maxValue = 0.0,
                ),
            feature = feature,
            parsedDate = DateTimeUtils.parseUtcToKst(data.timestamp),
            messageSender = messageSender,
            eventHistoryRepository = eventHistoryRepository,
            featureRepository = featureRepository,
            eventPublisher = eventPublisher,
            incidentService = incidentService,
        )
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
