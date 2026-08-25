package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.speaker.dto.SpeakerResponse
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
@RequestMapping("/speakers")
@Tag(name = "Speaker Controller", description = "스피커(TTS) 장치 API")
class SpeakerController(
    private val speakerService: SpeakerService,
) {
    @Operation(summary = "스피커 목록 조회", description = "등록된 스피커 목록을 조회합니다. 현장 아이디로 필터링할 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
        ],
    )
    @GetMapping
    fun getSpeakers(
        @Parameter(description = "현장 아이디", example = "1")
        @RequestParam("siteId", required = false) siteId: Long?,
    ): ResponseEntity<DataResponseBody<List<SpeakerResponse>>> = ResponseEntity.ok(DataResponseBody(speakerService.findAll(siteId)))
}
