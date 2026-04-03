package com.pluxity.aiot.eds

import com.pluxity.aiot.data.dto.ListDataResponse
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.eds.dto.CrowdCountLatestResponse
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctvs")
@ConditionalOnProperty("eds.enabled", havingValue = "true")
@Tag(name = "CCTV Crowd Count", description = "CCTV 군중계수 조회 API")
class EdsCrowdCountController(
    private val edsCrowdCountService: EdsCrowdCountService,
) {
    @Operation(summary = "군중계수 시계열 조회", description = "CCTV ID로 군중계수 데이터를 시간별로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "데이터 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "해당 ID의 CCTV를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}/crowd-count")
    fun getTimeSeries(
        @Parameter(description = "CCTV ID", required = true) @PathVariable id: Long,
        @Parameter(description = "데이터 집계 간격", example = "HOUR")
        @RequestParam(defaultValue = "HOUR", required = false) interval: DataInterval,
        @Parameter(description = "조회 시작일(yyyyMMddHHmmss)", required = true)
        @RequestParam("from") from: String,
        @Parameter(description = "조회 종료일(yyyyMMddHHmmss)", required = true)
        @RequestParam("to") to: String,
    ): ResponseEntity<DataResponseBody<ListDataResponse>> {
        val data = edsCrowdCountService.getTimeSeries(id, interval, from, to)
        return ResponseEntity.ok(DataResponseBody(data))
    }

    @Operation(summary = "군중계수 최신값 조회", description = "CCTV ID로 가장 최근 군중계수 데이터를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "최신 데이터 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "해당 ID의 CCTV를 찾을 수 없음 또는 데이터 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}/crowd-count/latest")
    fun getLatest(
        @Parameter(description = "CCTV ID", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<CrowdCountLatestResponse>> {
        val data = edsCrowdCountService.getLatest(id)
        return ResponseEntity.ok(DataResponseBody(data))
    }
}
