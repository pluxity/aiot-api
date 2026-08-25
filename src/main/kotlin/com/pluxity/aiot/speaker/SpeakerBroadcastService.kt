package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.emptyPageResponse
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastRequest
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastResponse
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastSearchRequest
import com.pluxity.aiot.speaker.dto.SpeakerBroadcastSummaryResponse
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 업체 송출 API 호출과 이력 엔티티 연동은 후속 구현에서 채운다.
 */
@Service
class SpeakerBroadcastService {
    fun broadcast(request: SpeakerBroadcastRequest) {
        // TODO 프리셋 조회 · 업체 송출 API 호출 · 송출 이력 적재 필요
    }

    fun findAll(request: SpeakerBroadcastSearchRequest): PageResponse<SpeakerBroadcastSummaryResponse> =
        emptyPageResponse(request.page, request.size)

    fun findById(broadcastId: Long): SpeakerBroadcastResponse = throw CustomException(ErrorCode.NOT_FOUND_SPEAKER_BROADCAST, broadcastId)
}
