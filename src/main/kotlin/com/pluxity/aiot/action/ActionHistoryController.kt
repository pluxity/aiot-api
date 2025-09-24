package com.pluxity.aiot.action

import com.pluxity.aiot.global.annotation.ResponseCreated
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/action-histories")
@Tag(name = "Action History Controller", description = "액션 이력 관리 API")
class ActionHistoryController(
    private val actionHistoryService: ActionHistoryService,
) {
    @Operation(summary = "액션 이력 생성", description = "새로운 액션 이력을 생성합니다")
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
    @ResponseCreated(path = "/action-histories/{id}")
    @PostMapping
    fun create(
        @RequestBody request: ActionHistoryRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(actionHistoryService.save(request))

    @Operation(summary = "전체 액션 이력 조회", description = "모든 액션 이력을 조회합니다")
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
    fun findAll(): ResponseEntity<DataResponseBody<List<ActionHistoryResponse>>> =
        ResponseEntity.ok(DataResponseBody(actionHistoryService.findAll()))

    @Operation(summary = "액션 이력 상세 조회", description = "특정 아이디의 액션 이력을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "액션 히스토리를 찾을 수 없음",
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
    @GetMapping("/{actionHistoryId}")
    fun findById(
        @PathVariable actionHistoryId: Long,
    ): ResponseEntity<DataResponseBody<ActionHistoryResponse>> =
        ResponseEntity.ok(DataResponseBody(actionHistoryService.getById(actionHistoryId)))

    @Operation(summary = "디바이스별 액션 이력 조회", description = "특정 디바이스의 액션 이력을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 디바이스 ID",
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
    @GetMapping("/device/{deviceId}")
    fun findByDeviceIdAndEventName(
        @PathVariable deviceId: String,
    ): ResponseEntity<DataResponseBody<List<ActionHistoryResponse>>> =
        ResponseEntity.ok(DataResponseBody(actionHistoryService.findByDeviceIdAndEventName(deviceId)))

    @Operation(summary = "이벤트 히스토리별 액션 이력 조회", description = "특정 이벤트 히스토리의 액션 이력을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "이벤트 히스토리를 찾을 수 없음",
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
    @GetMapping("/event-history/{eventHistoryId}")
    fun findByEventHistoryId(
        @PathVariable eventHistoryId: Long,
    ): ResponseEntity<DataResponseBody<List<ActionHistoryResponse>>> =
        ResponseEntity.ok(DataResponseBody(actionHistoryService.findByEventHistory(eventHistoryId)))
}
