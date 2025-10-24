package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.global.annotation.ResponseCreated
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.system.event.condition.dto.EventConditionBatchRequest
import com.pluxity.aiot.system.event.condition.dto.EventConditionResponse
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/event-conditions")
@Tag(name = "Event Condition Controller", description = "이벤트 조건 관리 API")
class EventConditionController(
    private val eventConditionService: EventConditionService,
) {
    @Operation(summary = "이벤트 조건 목록 조회", description = "특정 objectId의 모든 이벤트 조건을 조회합니다")
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
    fun findAllByObjectId(
        @RequestParam objectId: String,
    ): ResponseEntity<DataResponseBody<List<EventConditionResponse>>> =
        ResponseEntity.ok(DataResponseBody(eventConditionService.findAllByObjectId(objectId)))

    @Operation(summary = "이벤트 조건 상세 조회", description = "특정 이벤트 조건을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "이벤트 조건을 찾을 수 없음",
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
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<EventConditionResponse>> = ResponseEntity.ok(DataResponseBody(eventConditionService.findById(id)))

    @Operation(summary = "이벤트 조건 일괄 생성", description = "새로운 이벤트 조건들을 일괄 생성합니다")
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
    @ResponseCreated("/event-conditions/{id}")
    @PostMapping
    fun createBatch(
        @RequestBody request: EventConditionBatchRequest,
    ): ResponseEntity<List<Long>> =
        ResponseEntity.ok(eventConditionService.createBatch(request))

    @Operation(summary = "이벤트 조건 일괄 수정", description = "기존 이벤트 조건들을 일괄 수정합니다 (기존 조건 삭제 후 재생성)")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
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
    @PutMapping
    fun updateBatch(
        @RequestBody request: EventConditionBatchRequest,
    ): ResponseEntity<Void> {
        eventConditionService.updateBatch(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "이벤트 조건 삭제", description = "이벤트 조건을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "이벤트 조건을 찾을 수 없음",
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
    @DeleteMapping("/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        eventConditionService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
