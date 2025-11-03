package com.pluxity.aiot.dashboard

import com.pluxity.aiot.global.response.DataResponseBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard Controller", description = "대시보드 API")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    @GetMapping("/sensor-summary")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ),
        ],
    )
    @Operation(summary = "디바이스 상태 정보 조회", description = "공원별 디바이스 상태를 조회합니다.")
    fun getSensorSummary(): ResponseEntity<DataResponseBody<List<SensorSummary>>> =
        ResponseEntity.ok(DataResponseBody(dashboardService.getSensorSummary()))
}
