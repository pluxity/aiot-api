package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.emptyPageResponse
import com.pluxity.aiot.speaker.dto.SpeakerPresetRequest
import com.pluxity.aiot.speaker.dto.SpeakerPresetResponse
import com.pluxity.aiot.speaker.dto.SpeakerPresetSearchRequest
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class SpeakerPresetService {
    fun findAll(request: SpeakerPresetSearchRequest): PageResponse<SpeakerPresetResponse> = emptyPageResponse(request.page, request.size)

    fun findById(presetId: Long): SpeakerPresetResponse = throw CustomException(ErrorCode.NOT_FOUND_SPEAKER_PRESET, presetId)

    fun save(request: SpeakerPresetRequest): Long {
        // TODO 프리셋 생성 구현 필요
        return 0L
    }

    fun update(
        presetId: Long,
        request: SpeakerPresetRequest,
    ) {
        // TODO 프리셋 수정 구현 필요
    }

    fun delete(presetId: Long) {
        // TODO 프리셋 삭제 구현 필요
    }
}
