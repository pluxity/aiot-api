package com.pluxity.aiot.alarm.service

import com.pluxity.aiot.alarm.dto.SubscriptionSgnResponse
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class SensorDataHandler(
    deviceTypeRepository: DeviceTypeRepository,
    processors: List<SensorDataProcessor>,
    private val featureRepository: FeatureRepository,
) {
    private val processorMap: Map<String, SensorDataProcessor> =
        processors.associateBy { it.getObjectId() }

    private val deviceTypeCache =
        deviceTypeRepository
            .findAll()
            .associateBy { it.objectId }
            .toMutableMap()

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
        val deviceType =
            deviceTypeCache[objectId] ?: run {
                log.warn { "ObjectId에 해당하는 deviceType이 없습니다: $objectId" }
                return
            }
        val processor =
            processorMap[objectId] ?: run {
                log.warn { "ObjectId에 해당하는 프로세서가 없습니다: $objectId" }
                return
            }

        processor.process(deviceId, deviceType, feature.facility?.id!!, sgn.nev.rep.cin.con)
    }

    fun updateDeviceTypeCache(deviceType: DeviceType) {
        deviceTypeCache[deviceType.objectId] = deviceType
        log.debug { "DeviceType cache updated for objectId: ${deviceType.objectId}" }
    }

    fun removeDeviceTypeFromCache(objectId: String?) {
        deviceTypeCache.remove(objectId)
        log.debug { "DeviceType removed from cache for objectId: $objectId" }
    }
}
