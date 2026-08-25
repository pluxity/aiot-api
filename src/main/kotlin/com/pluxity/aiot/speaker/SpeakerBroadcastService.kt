package com.pluxity.aiot.speaker

import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.broadcast.dto.BroadcastResult
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastRequest
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastResponse
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastSearchRequest
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastSummaryResponse
import org.springframework.stereotype.Service

/**
 * TODO 스펙 공유용 스텁. 업체 송출 API 호출과 이력 엔티티 연동은 후속 구현에서 채운다.
 */
@Service
class SpeakerBroadcastService {
    fun broadcast(request: SpeakerBroadcastRequest) {
        // TODO 프리셋 조회 · 업체 송출 API 호출 · 송출 이력 적재 필요
    }

    fun findAll(request: SpeakerBroadcastSearchRequest): PageResponse<SpeakerBroadcastSummaryResponse> {
        val filtered = SUMMARY_SAMPLES.filter { request.userId.isNullOrBlank() || it.userId == request.userId }

        return PageResponse(
            content = filtered,
            pageNumber = request.page,
            pageSize = request.size,
            totalElements = filtered.size.toLong(),
            last = true,
            first = request.page == 1,
        )
    }

    fun findById(broadcastId: Long): SpeakerBroadcastResponse = DETAIL_SAMPLE.copy(id = broadcastId)

    companion object {
        private val RESULT_SAMPLES =
            SpeakerService.SAMPLES.map { speaker ->
                val success = speaker.status == DeviceStatus.NORMAL
                BroadcastResult(
                    targetId = speaker.id,
                    targetName = speaker.name,
                    siteId = speaker.site?.id,
                    siteName = speaker.site?.name,
                    success = success,
                    failureReason = if (success) null else "장치가 응답하지 않습니다.",
                )
            }

        private val SUMMARY_SAMPLES =
            listOf(
                SpeakerBroadcastSummaryResponse(
                    id = 1L,
                    message = "잠시 후 공원이 폐장합니다. 이용에 참고해 주시기 바랍니다.",
                    presetId = 1L,
                    presetTitle = "폐장 안내",
                    userId = "admin",
                    broadcastAt = "2026-08-25T09:00:00",
                    totalCount = RESULT_SAMPLES.size,
                    successCount = RESULT_SAMPLES.count { it.success },
                    failureCount = RESULT_SAMPLES.count { !it.success },
                ),
                SpeakerBroadcastSummaryResponse(
                    id = 2L,
                    message = "미아를 찾고 있습니다. 안내센터로 방문해 주시기 바랍니다.",
                    presetId = null,
                    presetTitle = null,
                    userId = "admin",
                    broadcastAt = "2026-08-25T10:30:00",
                    totalCount = 1,
                    successCount = 1,
                    failureCount = 0,
                ),
            )

        private val DETAIL_SAMPLE =
            SUMMARY_SAMPLES.first().let {
                SpeakerBroadcastResponse(
                    id = it.id,
                    message = it.message,
                    presetId = it.presetId,
                    presetTitle = it.presetTitle,
                    userId = it.userId,
                    broadcastAt = it.broadcastAt,
                    totalCount = it.totalCount,
                    successCount = it.successCount,
                    failureCount = it.failureCount,
                    results = RESULT_SAMPLES,
                )
            }
    }
}
