package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsEventResponse
import com.pluxity.aiot.eds.dto.EdsStreamResult
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctvs")
@Tag(name = "CCTV Controller", description = "CCTV 관리 API")
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsController(
    private val edsService: EdsService,
    private val edsEventService: EdsEventService,
) {
    @Operation(summary = "EDS 이벤트 목록 조회", description = "EDS 이벤트 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이벤트 목록 조회 성공"),
        ],
    )
    @GetMapping("/events")
    fun getEvents(
        @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "9999") size: Int,
        @Parameter(description = "조회 시작일시 (yyyyMMddHHmmss)") @RequestParam(required = false) from: String?,
        @Parameter(description = "조회 종료일시 (yyyyMMddHHmmss)") @RequestParam(required = false) to: String?,
    ): ResponseEntity<DataResponseBody<PageResponse<EdsEventResponse>>> =
        ResponseEntity.ok(DataResponseBody(edsEventService.findAll(page, size, from, to)))

    @Operation(summary = "EDS 카메라 동기화", description = "EDS 서버의 카메라 목록을 조회하여 CCTV 테이블과 동기화합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "EDS 카메라 동기화 성공"),
        ],
    )
    @PostMapping("/sync")
    fun sync(): ResponseEntity<Void> {
        edsService.sync()
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "실시간 영상 재생 URL 요청", description = "카메라 ID로 실시간 영상 스트림 URL을 요청합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "실시간 스트림 URL 조회 성공"),
        ],
    )
    @GetMapping("/{cameraId}/stream/realtime")
    fun getRealtimeStreamUrl(
        @Parameter(description = "카메라 ID", required = true) @PathVariable cameraId: String,
    ): ResponseEntity<DataResponseBody<EdsStreamResult>> = ResponseEntity.ok(DataResponseBody(edsService.getRealtimeStreamUrl(cameraId)))

    @Operation(summary = "녹화 영상 재생 URL 요청", description = "카메라 ID로 녹화 영상 스트림 URL을 요청합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "녹화 스트림 URL 조회 성공"),
        ],
    )
    @GetMapping("/{cameraId}/stream/record")
    fun getRecordStreamUrl(
        @Parameter(description = "카메라 ID", required = true) @PathVariable cameraId: String,
        @Parameter(description = "녹화 시작 시간 (yyyyMMddHHmmss)", required = true) @RequestParam recordStartTime: String,
        @Parameter(description = "녹화 종료 시간 (yyyyMMddHHmmss)", required = true) @RequestParam recordEndTime: String,
    ): ResponseEntity<DataResponseBody<EdsStreamResult>> =
        ResponseEntity.ok(
            DataResponseBody(
                edsService.getRecordStreamUrl(
                    cameraId,
                    recordStartTime,
                    recordEndTime,
                ),
            ),
        )
}
