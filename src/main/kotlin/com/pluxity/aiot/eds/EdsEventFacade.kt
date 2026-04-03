package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsCrowdCountData
import com.pluxity.aiot.eds.dto.EdsEventData
import com.pluxity.aiot.file.service.FileService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsEventFacade(
    private val edsClient: EdsClient,
    private val edsEventService: EdsEventService,
    private val edsCrowdCountService: EdsCrowdCountService,
    private val fileService: FileService,
) {
    fun processEvent(eventData: EdsEventData) {
        val thumbnailBytes = edsClient.getEventThumbnail(eventData.index)
        val thumbnailFileId =
            thumbnailBytes?.let {
                fileService.initiateUpload(it, "eds-event-${eventData.index}.jpg", "image/jpeg")
            }
        edsEventService.saveEvent(eventData, thumbnailFileId)
    }

    fun processCrowdCount(crowdCountData: EdsCrowdCountData) {
        edsCrowdCountService.save(crowdCountData)
    }
}
