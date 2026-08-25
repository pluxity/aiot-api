package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayPresetRequest
import com.pluxity.aiot.display.dto.DisplayPresetResponse
import com.pluxity.aiot.display.dto.DisplayPresetSearchRequest
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.emptyPageResponse
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class DisplayPresetService {
    fun findAll(request: DisplayPresetSearchRequest): PageResponse<DisplayPresetResponse> = emptyPageResponse(request.page, request.size)

    fun findById(presetId: Long): DisplayPresetResponse = throw CustomException(ErrorCode.NOT_FOUND_DISPLAY_PRESET, presetId)

    fun save(request: DisplayPresetRequest): Long {
        // TODO 프리셋 생성 구현 필요
        return 0L
    }

    fun update(
        presetId: Long,
        request: DisplayPresetRequest,
    ) {
        // TODO 프리셋 수정 구현 필요
    }

    fun delete(presetId: Long) {
        // TODO 프리셋 삭제 구현 필요
    }
}
