package com.pluxity.aiot.alarm.service

import com.pluxity.aiot.alarm.dto.SubscriptionSgnResponse
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.feature.FeatureRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SensorDataHandler(
    processors: List<SensorDataProcessor>,
    private val featureRepository: FeatureRepository,
) {
    private val processorMap: Map<String, SensorDataProcessor> =
        processors.associateBy { it.getObjectId() }

    fun handleData(sgn: SubscriptionSgnResponse) {
        val sur = sgn.sur
        val surParts = sur.split("/")
        if (surParts.size < 4) {
            log.warn { "Invalid sur format: $sur" }
            return
        }
        val deviceId = surParts[2]
        val objectId = surParts[3].split("_")[0]
        val feature =
            featureRepository.findByDeviceId(deviceId) ?: run {
                log.warn { "deviceId에 해당하는 Feature가 없습니다: $deviceId" }
                return
            }

        val sensorType =
            try {
                SensorType.fromObjectId(objectId)
            } catch (e: IllegalArgumentException) {
                log.warn { "ObjectId에 해당하는 SensorType이 없습니다: $objectId, $e" }
                return
            }

        val processor =
            processorMap[objectId] ?: run {
                log.warn { "ObjectId에 해당하는 프로세서가 없습니다: $objectId" }
                return
            }

        processor.process(deviceId, sensorType, feature.site?.id!!, sgn.nev.rep.cin.con)
    }
}
