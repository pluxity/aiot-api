package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsEventData
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsEventFacade(
    private val edsClient: EdsClient,
    private val edsEventService: EdsEventService,
) {
    fun processEvent(eventData: EdsEventData) {
        val thumbnailBytes = edsClient.getEventThumbnail(eventData.index)
        edsEventService.saveEvent(eventData, thumbnailBytes)
    }
}
