package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.system.device.type.dto.DeviceTypeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/device-types")
@Tag(name = "Device Type Controller", description = "디바이스 종류 관리 API")
class DeviceTypeController(
    private val deviceTypeService: DeviceTypeService,
) {
    @Operation(summary = "디바이스 종류 목록 조회", description = "모든 디바이스 종류 목록을 조회합니다")
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
    fun findAll(): ResponseEntity<DataResponseBody<List<DeviceTypeResponse>>> =
        ResponseEntity.ok(DataResponseBody(deviceTypeService.findAll()))
}
