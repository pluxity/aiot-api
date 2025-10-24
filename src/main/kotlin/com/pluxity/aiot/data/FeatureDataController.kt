package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.DataResponse
import com.pluxity.aiot.data.dto.ListDataResponse
import com.pluxity.aiot.data.enum.DataInterval
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/features")
@Tag(name = "Feature Controller", description = "피처 관리 API")
class FeatureDataController(
    private val dataService: DataService,
) {
    @Operation(summary = "센서 시간별 데이터 조회", description = "Device ID로 특정 센서의 데이터 정보를 시간별로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "데이터 조회 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 ID의 센서를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{deviceId}/time-series")
    fun getPeriodData(
        @Parameter(description = "Device ID", required = true) @PathVariable deviceId: String,
        @Parameter(description = "데이터 집계 간격", example = "HOUR")
        @RequestParam(defaultValue = "HOUR", required = false) interval: DataInterval,
        @Parameter(description = "조회 시작일(yyyyMMddHHmmss)", required = true)
        @RequestParam("from") from: String,
        @Parameter(description = "조회 종료일(yyyyMMddHHmmss)", required = true)
        @RequestParam("to") to: String,
    ): ResponseEntity<DataResponseBody<ListDataResponse>> =
        ResponseEntity.ok(DataResponseBody(dataService.getFeatureTimeSeries(deviceId, interval, from, to)))

    @Operation(summary = "센서 최근 데이터 조회", description = "ID로 특정 센서의 최근 데이터 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "최근 데이터 조회 성공",
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 ID의 센서를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "최근 데이터가 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{deviceId}/latest")
    fun getLatestData(
        @Parameter(description = "Device ID", required = true) @PathVariable deviceId: String,
    ): ResponseEntity<DataResponseBody<DataResponse>> = ResponseEntity.ok(DataResponseBody(dataService.getFeatureLatestData(deviceId)))
}
