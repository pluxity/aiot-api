package com.pluxity.aiot.dashboard

import com.pluxity.aiot.event.dto.EventResponse
import com.pluxity.aiot.event.entity.HistoryResult
import com.pluxity.aiot.global.response.DataResponseBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard Controller", description = "대시보드 API")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @Operation(summary = "디바이스 상태 정보 조회", description = "공원별 디바이스 상태를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
        ],
    )
    @GetMapping("/sensor-summary")
    fun getSensorSummary(): ResponseEntity<DataResponseBody<List<SensorSummary>>> =
        ResponseEntity.ok(DataResponseBody(dashboardService.getSensorSummary()))

    @Operation(summary = "이벤트 정보 조회", description = "공원별 이벤트 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(ref = "#/components/schemas/EventSummary"),
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/event-summary")
    fun getEventSummary(
        @Parameter(description = "조회 시작일(yyyyMMddHHmmss)", required = false)
        @RequestParam("from", required = false) from: String?,
        @Parameter(description = "조회 종료일(yyyyMMddHHmmss)", required = false)
        @RequestParam("to", required = false) to: String?,
    ): ResponseEntity<DataResponseBody<Map<HistoryResult, List<EventResponse>>>> =
        ResponseEntity.ok(DataResponseBody(dashboardService.getEventSummary(from, to)))
}
