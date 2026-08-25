package com.pluxity.aiot.display

import com.pluxity.aiot.broadcast.BroadcastStubSamples
import com.pluxity.aiot.display.dto.DisplayPresetRequest
import com.pluxity.aiot.display.dto.DisplayPresetResponse
import com.pluxity.aiot.display.dto.DisplayPresetSearchRequest
import com.pluxity.aiot.global.response.PageResponse
import org.springframework.stereotype.Service

/**
 * TODO 스펙 공유용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class DisplayPresetService {
    fun findAll(request: DisplayPresetSearchRequest): PageResponse<DisplayPresetResponse> {
        val filtered = SAMPLES.filter { request.title.isNullOrBlank() || it.title.contains(request.title) }

        return PageResponse(
            content = filtered,
            pageNumber = request.page,
            pageSize = request.size,
            totalElements = filtered.size.toLong(),
            last = true,
            first = request.page == 1,
        )
    }

    fun findById(presetId: Long): DisplayPresetResponse = SAMPLES.first().copy(id = presetId)

    fun save(request: DisplayPresetRequest): Long = 1L

    fun update(
        presetId: Long,
        request: DisplayPresetRequest,
    ) {
        // TODO 프리셋 수정 구현 필요
    }

    fun delete(presetId: Long) {
        // TODO 프리셋 삭제 구현 필요
    }

    companion object {
        private val SAMPLES =
            listOf(
                DisplayPresetResponse(
                    id = 1L,
                    title = "폐장 안내",
                    message = "잠시 후 공원이 폐장합니다. 이용에 참고해 주시기 바랍니다.",
                    baseResponse = BroadcastStubSamples.baseResponse,
                ),
                DisplayPresetResponse(
                    id = 2L,
                    title = "미아 찾기",
                    message = "미아를 찾고 있습니다. 안내센터로 방문해 주시기 바랍니다.",
                    baseResponse = BroadcastStubSamples.baseResponse,
                ),
            )
    }
}
