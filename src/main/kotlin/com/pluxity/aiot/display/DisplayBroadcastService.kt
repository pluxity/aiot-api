package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayBroadcastRequest
import com.pluxity.aiot.display.dto.DisplayBroadcastResponse
import com.pluxity.aiot.display.dto.DisplayBroadcastSearchRequest
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.emptyPageResponse
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 업체 송출 API 호출과 이력 엔티티 연동은 후속 구현에서 채운다.
 */
@Service
class DisplayBroadcastService {
    fun broadcast(request: DisplayBroadcastRequest) {
        // TODO 프리셋 조회 · 대상 전광판별 업체 송출 API 호출 · 장치 단위 송출 이력 적재 필요
    }

    fun findAll(request: DisplayBroadcastSearchRequest): PageResponse<DisplayBroadcastResponse> =
        emptyPageResponse(request.page, request.size)
}
