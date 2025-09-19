package com.pluxity.aiot.system.mobius

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.system.mobius.dto.MobiusRequest
import com.pluxity.aiot.system.mobius.dto.MobiusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mobius")
@Tag(name = "Mobius Controller", description = "플랫폼 연동 관리 API")
class MobiusConfigController(
    private val mobiusTransactionService: MobiusTransactionService,
    private val mobiusConfigService: MobiusConfigService,
) {
    @Operation(summary = "연동 정보 조회", description = "연동 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
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
    @GetMapping("/api-url")
    fun getApiUrl(): ResponseEntity<DataResponseBody<MobiusResponse>> =
        ResponseEntity.ok(DataResponseBody(mobiusTransactionService.loadLatestUrl()))

    @Operation(summary = "연동 정보 수정", description = "연동 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "정보 수정 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "약어를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ),
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
    @PostMapping("/api-url")
    fun changeApiUrl(
        @RequestBody @Valid request: MobiusRequest,
    ): ResponseEntity<Void> {
        // TODO 수정필요
//        aiotService.removeAllSubscriptions()
        mobiusConfigService.createUrl(request.url)
        return ResponseEntity.noContent().build()
    }
}
