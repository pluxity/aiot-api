package com.pluxity.aiot.action

import com.pluxity.aiot.global.annotation.ResponseCreated
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
@Tag(name = "Event Controller", description = "이벤트 관리 API")
class ActionHistoryController(
    private val actionHistoryService: ActionHistoryService,
) {
    @Operation(summary = "조치 이력 생성", description = "새로운 조치 이력을 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "생성 성공"),
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
    @ResponseCreated(path = "/events/{eventId}/action-histories/{id}")
    @PostMapping("/{eventId}/action-histories")
    fun create(
        @Parameter(description = "이벤트 ID", required = true) @PathVariable eventId: Long,
        @RequestBody request: ActionHistoryRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(actionHistoryService.save(eventId, request))

    @Operation(summary = "이벤트별 조치 이력 조회", description = "이벤트별 조치 이력을 조회합니다")
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
    @GetMapping("/{eventId}/action-histories")
    fun findAll(
        @Parameter(description = "이벤트 ID", required = true) @PathVariable eventId: Long,
    ): ResponseEntity<DataResponseBody<List<ActionHistoryResponse>>> =
        ResponseEntity.ok(DataResponseBody(actionHistoryService.findAll(eventId)))

    @Operation(summary = "조치 이력 수정", description = "조치 이력을 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "수정 성공",
            ), ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ), ApiResponse(
                responseCode = "404",
                description = "해당 ID의 조치 이력을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping("/{eventId}/action-histories/{id}")
    fun update(
        @Parameter(description = "이벤트 ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "조치 이력 ID", required = true) @PathVariable id: Long,
        @RequestBody request: ActionHistoryRequest,
    ): ResponseEntity<Void> {
        actionHistoryService.update(eventId, id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "조치 이력 삭제", description = "ID로 조치 이력을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "삭제 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "해당 ID의 조치 이력을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @DeleteMapping("/{eventId}/action-histories/{id}")
    fun delete(
        @Parameter(description = "이벤트 ID", required = true) @PathVariable eventId: Long,
        @Parameter(description = "조치 이력 ID", required = true) @PathVariable id: Long,
    ): ResponseEntity<Void> {
        actionHistoryService.delete(eventId, id)
        return ResponseEntity.noContent().build()
    }
}
