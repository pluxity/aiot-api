package com.pluxity.aiot.incident

import com.pluxity.aiot.event.EventStatusChangeNotifier
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.site.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class IncidentService(
    private val incidentRepository: IncidentRepository,
    private val siteRepository: SiteRepository,
    private val featureRepository: FeatureRepository,
    private val eventStatusChangeNotifier: EventStatusChangeNotifier,
) {
    @Transactional
    fun open(
        sourceType: IncidentSourceType,
        sourceId: Long,
        site: Site?,
        deviceId: String,
        deviceName: String?,
        title: String,
        level: ConditionLevel,
        occurredAt: LocalDateTime,
        latitude: Double?,
        longitude: Double?,
        guideMessage: String? = null,
    ): Incident =
        incidentRepository.save(
            Incident(
                sourceType = sourceType,
                sourceId = sourceId,
                site = site ?: findSiteByPoint(longitude, latitude),
                deviceId = deviceId,
                deviceName = deviceName,
                title = title,
                level = level,
                occurredAt = occurredAt,
                latitude = latitude,
                longitude = longitude,
                guideMessage = guideMessage,
            ),
        )

    @Transactional
    fun changeStatus(
        incident: Incident,
        status: EventStatus,
    ) {
        incident.changeStatus(status)
        if (status == EventStatus.RESOLVED) syncFeatureEventStatus(incident)
        eventStatusChangeNotifier.notifyStatusChanged(incident)
    }

    /**
     * 조치 완료 후에도 feature가 경보 상태로 남아 있으면 SensorDataProcessor의 동일 상태 가드에 걸려
     * 같은 레벨 경보가 재감지돼도 새 incident가 생기지 않는다. 남은 미조치 incident의 최고 레벨까지만 내린다.
     * 미조치 incident는 과거 이력이지 현재 계측 상태가 아니므로, 센서가 이미 정상으로 돌아와 있으면 다시 올리지 않는다.
     */
    private fun syncFeatureEventStatus(incident: Incident) {
        if (incident.sourceType != IncidentSourceType.SENSOR) return
        val feature = featureRepository.findByDeviceId(incident.deviceId) ?: return
        val current = ConditionLevel.entries.find { it.name == feature.eventStatus } ?: return
        if (current == ConditionLevel.DISCONNECTED) return

        // 방금 바꾼 상태가 아직 flush되지 않았을 수 있으므로 조회 결과에서 자기 자신을 제외한다
        val remaining =
            incidentRepository
                .findAllByDeviceIdAndSourceTypeAndStatusNot(incident.deviceId, IncidentSourceType.SENSOR, EventStatus.RESOLVED)
                .filter { it.id != incident.id }
                .maxByOrNull { it.level.priority }
                ?.level ?: ConditionLevel.NORMAL
        if (remaining.priority < current.priority) feature.updateEventStatus(remaining.name)
    }

    private fun findSiteByPoint(
        longitude: Double?,
        latitude: Double?,
    ): Site? {
        if (longitude == null || latitude == null) return null
        return siteRepository.findFirstByPointInPolygon(longitude, latitude)
    }
}
