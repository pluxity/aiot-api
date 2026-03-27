package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsEventData
import com.pluxity.aiot.file.service.FileService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsEventService(
    private val edsEventRepository: EdsEventRepository,
    private val fileService: FileService,
) {
    @Transactional
    fun saveEvent(eventData: EdsEventData, thumbnailBytes: ByteArray?) {
        val existing = edsEventRepository.findByEventId(eventData.id)

        if (existing != null) {
            existing.updateOnEnd(eventData.eventEnd, eventData.frameTime, eventData.status)
            return
        }

        val thumbnailFileId = thumbnailBytes?.let {
            fileService.initiateUpload(it, "eds-event-${eventData.index}.jpg", "image/jpeg")
        }

        edsEventRepository.save(
            EdsEvent(
                index = eventData.index,
                eventId = eventData.id,
                profileName = eventData.profileName,
                cameraId = eventData.cameraId,
                eventType = eventData.type,
                eventStart = eventData.eventStart,
                eventEnd = eventData.eventEnd,
                frameTime = eventData.frameTime,
                status = eventData.status,
                eventZoneId = eventData.eventZoneId,
                eventZoneName = eventData.eventZoneName,
                latitude = eventData.latitude,
                longitude = eventData.longitude,
                detectedVehicleNumber = eventData.detectedVehicleNumber,
                thumbnailFileId = thumbnailFileId,
            ),
        )
    }
}
