package com.pluxity.aiot.mic

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.mic.dto.MicEventResponse
import com.pluxity.aiot.mic.dto.MicResponse
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mics")
@Tag(name = "AI Mic Controller", description = "AI 마이크(Sound Box) 관리 API")
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicController(
    private val micService: MicService,
    private val micEventService: MicEventService,
    private val micFacade: MicFacade,
) {
    @Operation(summary = "AI 마이크 목록 조회", description = "연동된 AI 마이크 장비 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "마이크 목록 조회 성공"),
        ],
    )
    @GetMapping
    fun getMics(): ResponseEntity<DataResponseBody<List<MicResponse>>> = ResponseEntity.ok(DataResponseBody(micService.findAll()))

    @Operation(summary = "AI 마이크 이벤트 목록 조회", description = "AI 마이크 이벤트 목록을 페이징하여 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "이벤트 목록 조회 성공"),
        ],
    )
    @GetMapping("/events")
    fun getEvents(
        @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam(defaultValue = "1") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "9999") size: Int,
        @Parameter(description = "업체 장비 식별자") @RequestParam(required = false) micId: String?,
        @Parameter(description = "조회 시작일시 (yyyyMMddHHmmss)") @RequestParam(required = false) from: String?,
        @Parameter(description = "조회 종료일시 (yyyyMMddHHmmss)") @RequestParam(required = false) to: String?,
    ): ResponseEntity<DataResponseBody<PageResponse<MicEventResponse>>> =
        ResponseEntity.ok(DataResponseBody(micEventService.findAll(page, size, micId, from, to)))

    @Operation(summary = "AI 마이크 동기화", description = "업체 서버의 마이크 목록을 조회하여 마이크 테이블과 동기화합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "마이크 동기화 성공"),
            ApiResponse(
                responseCode = "502",
                description = "업체 API 호출 실패",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @PostMapping("/sync")
    fun sync(): ResponseEntity<Void> {
        micFacade.sync()
        return ResponseEntity.noContent().build()
    }
}
