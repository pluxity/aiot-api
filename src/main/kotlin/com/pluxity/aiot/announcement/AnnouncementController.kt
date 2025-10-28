package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.AnnouncementResponse
import com.pluxity.aiot.announcement.dto.BroadcastRequest
import com.pluxity.aiot.announcement.dto.SearchRequest
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.global.response.PageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/announcements")
@Tag(name = "Announcement Controller", description = "안내방송 API")
class AnnouncementController(
    private val announcementService: AnnouncementService,
) {
    @Operation(summary = "안내방송 송출", description = "안내방송을 송출합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "송출 성공"),
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
        @RequestBody request: BroadcastRequest,
    ): ResponseEntity<Void> {
        announcementService.broadcast(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "안내방송 이력 조회", description = "안내방송 이력을 조회합니다")
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
    fun findAll(request: SearchRequest): ResponseEntity<DataResponseBody<PageResponse<AnnouncementResponse>>> =
        ResponseEntity.ok(DataResponseBody(announcementService.findAll(request)))
}
