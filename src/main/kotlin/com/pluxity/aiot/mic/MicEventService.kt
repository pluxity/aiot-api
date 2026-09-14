package com.pluxity.aiot.mic

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.incident.IncidentSourceType
import com.pluxity.aiot.mic.dto.MicEventData
import com.pluxity.aiot.mic.dto.MicEventResponse
import com.pluxity.aiot.mic.dto.toResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("mic.enabled", havingValue = "true")
@Transactional(readOnly = true)
class MicEventService(
    private val micEventRepository: MicEventRepository,
    private val micRepository: MicRepository,
    private val incidentService: IncidentService,
) {
    fun findAll(
        page: Int,
        size: Int,
        micId: String?,
        from: String?,
        to: String?,
    ): PageResponse<MicEventResponse> {
        val pageable = PageRequest.of(page - 1, size)
        return micEventRepository.findAllByFilter(pageable, micId, from, to).toPageResponse { it.toResponse() }
    }

    @Transactional
    fun saveEvent(eventData: MicEventData) {
        val micId = eventData.mic?.id
        if (micId == null) {
            log.warn { "AI 마이크 이벤트에 마이크 정보가 없어 저장하지 않습니다: eventId=${eventData.id}" }
            return
        }

        if (micEventRepository.existsByEventId(eventData.id)) {
            log.debug { "이미 저장된 AI 마이크 이벤트입니다: eventId=${eventData.id}" }
            return
        }

        val noises = eventData.noises

        val saved =
            micEventRepository.save(
                MicEvent(
                    eventId = eventData.id,
                    micId = micId,
                    micName = eventData.mic.name,
                    labelId = eventData.label?.id,
                    labelNameKo = eventData.label?.name?.ko,
                    labelNameEn = eventData.label?.name?.en,
                    confidence = eventData.confidence,
                    latitude = eventData.mic.location?.latitude,
                    longitude = eventData.mic.location?.longitude,
                    noises = noises,
                    maxNoise = noises?.maxOrNull(),
                    avgNoise = noises?.takeIf { it.isNotEmpty() }?.average(),
                    occurredAt = parseOccurredAt(eventData.createdAt),
                ),
            )

        val mic = micRepository.findByVendorMicId(micId)
        incidentService.open(
            sourceType = IncidentSourceType.MIC,
            sourceId = saved.requiredId,
            site = mic?.site,
            deviceId = micId,
            deviceName = saved.micName ?: mic?.name,
            title = saved.labelNameKo ?: saved.labelNameEn ?: "소음 감지",
            level = ConditionLevel.WARNING,
            occurredAt = saved.occurredAt,
            latitude = saved.latitude ?: mic?.latitude,
            longitude = saved.longitude ?: mic?.longitude,
        )
    }

    /** 벤더는 created_at을 UTC ISO-8601로 준다 */
    private fun parseOccurredAt(createdAt: String?): LocalDateTime {
        if (createdAt == null) return LocalDateTime.now()
        return try {
            OffsetDateTime.parse(createdAt).atZoneSameInstant(KST).toLocalDateTime()
        } catch (e: Exception) {
            log.warn { "AI 마이크 이벤트 created_at 파싱 실패, 수신 시각으로 대체합니다: $createdAt (${e.message})" }
            LocalDateTime.now()
        }
    }

    companion object {
        private val KST = ZoneId.of("Asia/Seoul")
    }
}
