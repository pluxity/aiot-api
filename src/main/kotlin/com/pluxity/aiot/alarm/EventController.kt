package com.pluxity.aiot.alarm

import com.pluxity.aiot.alarm.dto.EventResponse
import com.pluxity.aiot.alarm.entity.HistoryResult
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Event Controller", description = "이벤트 관리 API")
class EventController(
    private val eventService: EventService,
) {
    @Operation(summary = "이벤트 목록 조회", description = "이벤트 목록 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "데이터 조회 성공",
            ),
        ],
    )
    @GetMapping
    fun getAll(
        @Parameter(description = "조회 시작일(yyyyMMddHHmmss)", required = false)
        @RequestParam("from", required = false) from: String?,
        @Parameter(description = "조회 종료일(yyyyMMddHHmmss)", required = false)
        @RequestParam("to", required = false) to: String?,
        @Parameter(description = "현장 아이디", required = false)
        @RequestParam("siteId", required = false) siteId: Long?,
        @Parameter(description = "이벤트 상태", required = false)
        @RequestParam("result", required = false) result: HistoryResult?,
    ): ResponseEntity<DataResponseBody<List<EventResponse>>> =
        ResponseEntity.ok(DataResponseBody(eventService.findAll(from, to, siteId, result)))

    @Operation(summary = "이벤트 상태 수정", description = "ID로 특정 이벤트의 상태를 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "수정 성공",
            ), ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ), ApiResponse(
                responseCode = "404",
                description = "해당 ID를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping("/{id}/status")
    fun update(
        @Parameter(description = "이벤트 ID", required = true) @PathVariable id: Long,
        @Parameter(description = "이벤트 상태", required = false)
        @RequestParam("result", required = false) result: HistoryResult,
    ): ResponseEntity<Void> {
        eventService.updateStatus(id, result)
        return ResponseEntity.noContent().build()
    }
}
