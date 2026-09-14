package com.pluxity.aiot.event

import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.ChangeEventStatusPayload
import com.pluxity.aiot.incident.Incident
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class EventStatusChangeNotifier(
    private val siteRepository: SiteRepository,
    private val messageSender: StompMessageSender,
) {
    fun notifyStatusChanged(incident: Incident) {
        val site =
            incident.site ?: findSiteByPoint(incident)
                ?: run {
                    log.warn { "현장을 알 수 없어 상태 변경 알림을 보내지 않습니다 (incidentId=${incident.requiredId})" }
                    return
                }

        messageSender.changeEventStatus(
            ChangeEventStatusPayload(
                siteId = site.requiredId,
                eventId = incident.requiredId,
                status = incident.status.name,
            ),
        )
    }

    private fun findSiteByPoint(incident: Incident) =
        incident.longitude?.let { lon ->
            incident.latitude?.let { lat -> siteRepository.findFirstByPointInPolygon(lon, lat) }
        }
}
