package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsEventData
import com.pluxity.aiot.eds.dto.EdsEventResponse
import com.pluxity.aiot.eds.dto.toResponse
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.file.extensions.getFileMapById
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
@Transactional(readOnly = true)
class EdsEventService(
    private val edsEventRepository: EdsEventRepository,
    private val fileService: FileService,
) {
    fun findAll(
        page: Int,
        size: Int,
        from: String?,
        to: String?,
    ): PageResponse<EdsEventResponse> {
        val pageable = PageRequest.of(page - 1, size)
        val events = edsEventRepository.findAllByFilter(pageable, from, to)
        val fileMap = fileService.getFileMapById(events.content) { it.thumbnailFileId }
        return events.toPageResponse { it.toResponse(fileMap[it.thumbnailFileId] ?: FileResponse()) }
    }

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

        thumbnailFileId?.let {
            fileService.finalizeUpload(it, "eds/events/eds-event-${eventData.index}.jpg")
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
