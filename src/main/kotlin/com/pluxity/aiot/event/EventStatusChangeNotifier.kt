package com.pluxity.aiot.event

import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.ChangeEventStatusPayload
import com.pluxity.aiot.site.SiteRepository
import org.springframework.stereotype.Component

@Component
class EventStatusChangeNotifier(
    private val siteRepository: SiteRepository,
    private val messageSender: StompMessageSender,
) {
    fun notifyStatusChanged(
        eventHistory: EventHistory,
        eventId: Long,
        status: String,
    ) {
        val longitude =
            requireNotNull(eventHistory.longitude) { "EventHistory.longitude must not be null" }
        val latitude =
            requireNotNull(eventHistory.latitude) { "EventHistory.latitude must not be null" }

        val site =
            requireNotNull(
                siteRepository.findFirstByPointInPolygon(lon = longitude, lat = latitude),
            ) { "Site must exist for lon=$longitude, lat=$latitude" }

        messageSender.changeEventStatus(
            ChangeEventStatusPayload(
                siteId = site.id!!,
                eventId = eventId,
                status = status,
            ),
        )
    }
}
