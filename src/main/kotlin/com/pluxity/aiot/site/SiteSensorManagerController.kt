package com.pluxity.aiot.site

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.dto.SiteSensorManagerRequest
import com.pluxity.aiot.site.dto.SiteSensorManagerResponse
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/sites/{siteId}/managers")
@Tag(name = "Site Sensor Manager Controller", description = "현장 센서 카테고리별 담당자 관리 API")
class SiteSensorManagerController(
    private val siteSensorManagerService: SiteSensorManagerService,
) {
    @Operation(
        summary = "담당자 지정 현황 조회",
        description = "현장의 센서 카테고리별 담당자를 조회합니다. 담당자가 없는 카테고리도 빈 목록으로 포함됩니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "현장을 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping
    fun getManagers(
        @Parameter(description = "현장 ID", required = true) @PathVariable siteId: Long,
    ): ResponseEntity<DataResponseBody<List<SiteSensorManagerResponse>>> =
        ResponseEntity.ok(DataResponseBody(siteSensorManagerService.findBySite(siteId)))

    @Operation(
        summary = "카테고리 담당자 지정",
        description = "해당 카테고리의 담당자를 요청 목록으로 전체 교체합니다. 빈 배열을 보내면 담당자가 모두 해제됩니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "지정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "현장 또는 사용자를 찾을 수 없음",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PutMapping("/{sensorType}")
    fun replaceManagers(
        @Parameter(description = "현장 ID", required = true) @PathVariable siteId: Long,
        @Parameter(description = "센서 카테고리", required = true) @PathVariable sensorType: SensorType,
        @Parameter(description = "담당자 목록", required = true) @Valid @RequestBody
        request: SiteSensorManagerRequest,
    ): ResponseEntity<Void> {
        siteSensorManagerService.replace(siteId, sensorType, request.userIds)
        return ResponseEntity.noContent().build()
    }
}
