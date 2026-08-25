package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayBroadcastRequest
import com.pluxity.aiot.display.dto.DisplayBroadcastResponse
import com.pluxity.aiot.display.dto.DisplayBroadcastSearchRequest
import com.pluxity.aiot.display.dto.DisplayBroadcastSummaryResponse
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.global.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/displays/broadcasts")
@Tag(name = "Display Broadcast Controller", description = "전광판(LED) 송출 · 송출 이력 API")
class DisplayBroadcastController(
    private val displayBroadcastService: DisplayBroadcastService,
) {
    @Operation(
        summary = "전광판 송출",
        description =
            "선택한 전광판으로 안내 메시지를 표출합니다. " +
                "프리셋(presetId)과 직접 입력(message) 중 하나를 지정하고, " +
                "송출 대상은 전광판 아이디(displayIds) 또는 현장 아이디(siteIds) 중 하나 이상을 지정합니다. " +
                "대상별 성공/실패는 송출 이력 상세 조회에서 확인합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "송출 요청 성공"),
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
                description = "프리셋 또는 전광판을 찾을 수 없음",
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
    @PostMapping
    fun broadcast(
        @Parameter(description = "송출 정보", required = true) @Valid @RequestBody
        request: DisplayBroadcastRequest,
    ): ResponseEntity<Void> {
        displayBroadcastService.broadcast(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "전광판 송출 이력 조회", description = "전광판 송출 이력을 페이징 조회합니다. 기간 · 송출자 · 현장으로 필터링할 수 있습니다.")
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
    @GetMapping
    fun getBroadcasts(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page", required = false) page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "10")
        @RequestParam("size", required = false) size: Int = 10,
        @Parameter(description = "시작일", example = "20260801")
        @RequestParam("from", required = false) from: String? = null,
        @Parameter(description = "종료일", example = "20260825")
        @RequestParam("to", required = false) to: String? = null,
        @Parameter(description = "송출자 아이디", example = "admin")
        @RequestParam("userId", required = false) userId: String? = null,
        @Parameter(description = "현장 아이디", example = "1")
        @RequestParam("siteId", required = false) siteId: Long? = null,
    ): ResponseEntity<DataResponseBody<PageResponse<DisplayBroadcastSummaryResponse>>> =
        ResponseEntity.ok(
            DataResponseBody(
                displayBroadcastService.findAll(DisplayBroadcastSearchRequest(page, size, from, to, userId, siteId)),
            ),
        )

    @Operation(
        summary = "전광판 송출 이력 상세 조회",
        description = "송출 이력 한 건을 조회합니다. 송출 대상별 성공/실패와 실패 사유를 포함합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "송출 이력을 찾을 수 없음",
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
    @GetMapping("/{broadcastId}")
    fun getBroadcast(
        @Parameter(description = "송출 이력 아이디", required = true) @PathVariable broadcastId: Long,
    ): ResponseEntity<DataResponseBody<DisplayBroadcastResponse>> =
        ResponseEntity.ok(DataResponseBody(displayBroadcastService.findById(broadcastId)))
}
