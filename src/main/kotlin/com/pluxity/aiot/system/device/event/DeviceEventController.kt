package com.pluxity.aiot.system.device.event

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.system.device.event.dto.DeviceEventRequest
import com.pluxity.aiot.system.device.event.dto.DeviceEventResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/device-types/{deviceTypeId}/events")
@Tag(name = "Device Event Controller", description = "디바이스 이벤트 관리 API")
class DeviceEventController(
    private val deviceEventService: DeviceEventService,
) {
    @Operation(summary = "디바이스 이벤트 목록 조회", description = "특정 디바이스 종류의 모든 이벤트 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "디바이스 종류를 찾을 수 없음",
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
    @GetMapping
    fun findAll(
        @PathVariable deviceTypeId: Long,
    ): ResponseEntity<DataResponseBody<List<DeviceEventResponse>>> =
        ResponseEntity.ok(DataResponseBody(deviceEventService.findAllByDeviceTypeId(deviceTypeId)))

    @Operation(summary = "디바이스 이벤트 상세 조회", description = "특정 디바이스 이벤트를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "디바이스 종류 또는 이벤트를 찾을 수 없음",
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
    @GetMapping("/{eventId}")
    fun findById(
        @PathVariable deviceTypeId: Long,
        @PathVariable eventId: Long,
    ): ResponseEntity<DataResponseBody<DeviceEventResponse>> =
        ResponseEntity.ok(DataResponseBody(deviceEventService.findById(deviceTypeId, eventId)))

    @Operation(
        summary = "디바이스 이벤트 생성/수정",
        description = "디바이스 이벤트와 이벤트 조건을 생성하거나 수정합니다. id가 없으면 생성, 있으면 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "생성/수정 성공"),
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
                description = "디바이스 종류를 찾을 수 없음",
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
    @PatchMapping
    fun createOrUpdate(
        @PathVariable deviceTypeId: Long,
        @RequestBody request: DeviceEventRequest,
    ): ResponseEntity<DataResponseBody<DeviceEventResponse>> =
        ResponseEntity.ok(DataResponseBody(deviceEventService.createOrUpdate(deviceTypeId, request)))

    @Operation(summary = "디바이스 이벤트 삭제", description = "디바이스 이벤트를 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "디바이스 종류 또는 이벤트를 찾을 수 없음",
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
    @DeleteMapping("/{eventId}")
    fun delete(
        @PathVariable deviceTypeId: Long,
        @PathVariable eventId: Long,
    ): ResponseEntity<Void> {
        deviceEventService.delete(deviceTypeId, eventId)
        return ResponseEntity.noContent().build()
    }
}
