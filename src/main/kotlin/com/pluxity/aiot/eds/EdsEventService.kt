package com.pluxity.aiot.eds

import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.eds.dto.EdsEventData
import com.pluxity.aiot.eds.dto.EdsEventResponse
import com.pluxity.aiot.eds.dto.toResponse
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.file.extensions.getFileMapById
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.incident.IncidentSourceType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
@Transactional(readOnly = true)
class EdsEventService(
    private val edsEventRepository: EdsEventRepository,
    private val fileService: FileService,
    private val cctvRepository: CctvRepository,
    private val incidentService: IncidentService,
) {
    companion object {
        private const val EDS_EVENTS: String = "eds-events/"
        private val EDS_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS")
    }

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
        val status = EdsEventStatus.fromCode(eventData.status)
        val existing = edsEventRepository.findByEventIdAndEventStatusNot(eventData.id, EdsEventStatus.ENDED)

        if (existing != null) {
            existing.updateOnEnd(
                eventData.eventEnd,
                eventData.frameTime,
                status ?: EdsEventStatus.ENDED,
            )
            return
        }

        val saveEntity =
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
                    eventStatus = status ?: EdsEventStatus.STARTED,
                    eventZoneId = eventData.eventZoneId,
                    eventZoneName = eventData.eventZoneName,
                    latitude = eventData.latitude,
                    longitude = eventData.longitude,
                    detectedVehicleNumber = eventData.detectedVehicleNumber,
                    thumbnailFileId = thumbnailFileId,
                ),
            )

        thumbnailFileId?.let {
            fileService.finalizeUpload(it, "${EDS_EVENTS}${saveEntity.requiredId}/")
        }

        val cctv = cctvRepository.findByEdsCameraId(eventData.cameraId)
        incidentService.open(
            sourceType = IncidentSourceType.CCTV,
            sourceId = saveEntity.requiredId,
            site = cctv?.site,
            deviceId = eventData.cameraId,
            deviceName = cctv?.name ?: eventData.cameraId,
            title = saveEntity.eventType?.description ?: eventData.profileName,
            level = ConditionLevel.WARNING,
            occurredAt = parseEventStart(eventData.eventStart),
            latitude = eventData.latitude ?: cctv?.latitude,
            longitude = eventData.longitude ?: cctv?.longitude,
        )
    }

    // 배포별로 EDS 포맷과 ISO가 섞여 온다. 하나로 줄이면 안 된다
    private fun parseEventStart(eventStart: String): LocalDateTime =
        runCatching { LocalDateTime.parse(eventStart, EDS_TIME_FORMAT) }
            .recoverCatching { OffsetDateTime.parse(eventStart).toLocalDateTime() }
            .recoverCatching { LocalDateTime.parse(eventStart) }
            .getOrElse {
                log.warn { "EDS 이벤트 시작 시각 파싱 실패, 수신 시각으로 대체합니다: $eventStart" }
                LocalDateTime.now()
            }
}
