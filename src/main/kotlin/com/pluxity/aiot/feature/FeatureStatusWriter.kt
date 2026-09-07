package com.pluxity.aiot.feature

import com.pluxity.aiot.data.dto.DeviceStatus
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/**
 * 동기화의 쓰기 단계.
 *
 * 오케스트레이션하는 빈과 분리해야 프록시를 타고 @Transactional이 적용된다.
 * 같은 빈 안에서 부르면 자기 호출이라 어노테이션이 아예 걸리지 않는다.
 *
 * 각 메서드는 넘겨받은 식별자로 엔티티를 이 트랜잭션 안에서 다시 읽는다.
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

    /**
     * Mobius가 알려준 경로 목록으로 Feature를 맞춘다.
     *
     * 목록이 비어 있으면 로컬 Feature가 전량 삭제되므로 호출 전에 걸러야 한다.
     */
    @Transactional
    fun applyPaths(uril: List<String>) {
        val objectIds = SensorType.entries.map { it.objectId }
        val existFeatures = featureRepository.findAll()
        val existFeatureMap = existFeatures.associateBy { it.deviceId }

        val features =
            uril
                .asSequence()
                .filter { path -> objectIds.any(path::contains) }
                .filter { it.count { char -> char == '/' } == 3 }
                .filterNot { it.contains("3_1.2_0") }
                .filterNot { it.contains("P-TST") }
                .map { path ->
                    val splitPaths = path.split("/")
                    val (deviceId, sensorId) = splitPaths[2] to splitPaths[3]
                    val deviceType = SensorType.fromObjectId(sensorId.take(5))
                    val parsedName =
                        parseDeviceName(deviceId, mapOf(deviceType.abbreviation.abbreviationKey to deviceType.abbreviation))
                    existFeatureMap[deviceId]?.apply {
                        updateInfo(parsedName, sensorId)
                    } ?: Feature(deviceId = deviceId, name = parsedName, objectId = sensorId)
                }.associateBy { it.deviceId }
                .values
                .toList()

        val removedIds = existFeatures.mapNotNull { it.deviceId } - features.map { it.deviceId }.toSet()
        featureRepository.deleteAllByDeviceIdIn(removedIds)
        featureRepository.saveAll(features)
    }
}
