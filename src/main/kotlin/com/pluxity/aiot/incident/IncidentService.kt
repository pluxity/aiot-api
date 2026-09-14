package com.pluxity.aiot.incident

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.site.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class IncidentService(
    private val incidentRepository: IncidentRepository,
    private val siteRepository: SiteRepository,
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

    private fun findSiteByPoint(
        longitude: Double?,
        latitude: Double?,
    ): Site? {
        if (longitude == null || latitude == null) return null
        return siteRepository.findFirstByPointInPolygon(longitude, latitude)
    }
}
