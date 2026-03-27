package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsEventData
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsEventService(
    private val edsEventRepository: EdsEventRepository,
) {
    @Transactional
    fun saveEvent(
        eventData: EdsEventData,
        thumbnailFileId: Long?,
    ) {
        val existing = edsEventRepository.findByEventIdAndEventStatusNot(eventData.id, EdsEventStatus.ENDED)

        if (existing != null) {
            existing.updateOnEnd(
                eventData.eventEnd,
                eventData.frameTime,
                EdsEventStatus.fromCode(eventData.status) ?: EdsEventStatus.ENDED,
            )
            return
        }

        edsEventRepository.save(
            EdsEvent(
                index = eventData.index,
                eventId = eventData.id,
                profileName = eventData.profileName,
                cameraId = eventData.cameraId,
                eventType = EdsEventType.fromCode(eventData.type),
                eventStart = eventData.eventStart,
                eventEnd = eventData.eventEnd,
                frameTime = eventData.frameTime,
                eventStatus = EdsEventStatus.fromCode(eventData.status) ?: EdsEventStatus.STARTED,
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
