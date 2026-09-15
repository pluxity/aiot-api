package com.pluxity.aiot.feature

import com.pluxity.aiot.data.MobiusDevice
import com.pluxity.aiot.data.dto.DeviceStatus
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * 오케스트레이션하는 빈과 분리해야 프록시를 타고 @Transactional이 적용된다.
 * 같은 빈 안에서 부르면 자기 호출이라 어노테이션이 아예 걸리지 않는다.
 */
@Service
class FeatureStatusWriter(
    private val featureRepository: FeatureRepository,
    private val siteRepository: SiteRepository,
) {
    @Transactional
    fun applyStatuses(statuses: Map<String, DeviceStatus>) {
        if (statuses.isEmpty()) return

        featureRepository.findAllByDeviceIdIn(statuses.keys.toList()).forEach { feature ->
            val status = statuses[feature.deviceId] ?: return@forEach
            val site = siteRepository.findFirstByPointInPolygon(status.longitude, status.latitude)
            feature.updateStatusInfo(status.longitude, status.latitude, status.batteryLevel, site)
        }
        log.info { "위치 동기화 반영 완료: ${statuses.size}건" }
    }

    @Transactional
    fun applyBatteryLevels(levels: Map<String, Int?>) {
        if (levels.isEmpty()) return

        featureRepository.findAllByDeviceIdIn(levels.keys.toList()).forEach { feature ->
            val level = levels[feature.deviceId]
            feature.updateBatteryLevel(level)
            log.info { "deviceId: ${feature.deviceId}, Battery Level: $level" }
        }
    }

    /** 목록이 비어 있으면 로컬 Feature가 전량 삭제되므로 호출 전에 걸러야 한다. */
    @Transactional
    fun applyDevices(devices: List<MobiusDevice>) {
        val existFeatures = featureRepository.findAll()
        val existFeatureMap = existFeatures.associateBy { it.deviceId }

        val features =
            devices
                .map { device ->
                    val deviceType = SensorType.fromObjectId(device.objectId.take(5))
                    val parsedName =
                        parseDeviceName(device.deviceId, mapOf(deviceType.abbreviation.abbreviationKey to deviceType.abbreviation))
                    existFeatureMap[device.deviceId]?.apply {
                        updateInfo(parsedName, device.objectId)
                    } ?: Feature(deviceId = device.deviceId, name = parsedName, objectId = device.objectId)
                }

        val removedIds = existFeatures.mapNotNull { it.deviceId } - devices.map { it.deviceId }.toSet()
        featureRepository.deleteAllByDeviceIdIn(removedIds)
        featureRepository.saveAll(features)
    }
}
