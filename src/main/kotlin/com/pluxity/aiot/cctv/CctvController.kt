package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvCoordinateRequest
import com.pluxity.aiot.cctv.dto.CctvResponse
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctvs")
@Tag(name = "CCTV Controller", description = "CCTV 관리 API")
class CctvController(
    private val cctvService: CctvService,
) {
    @GetMapping
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
            ),
        ],
    )
    @Operation(summary = "CCTV 목록 조회", description = "모든 CCTV 목록을 조회합니다.")
    fun getAll(
        @Parameter(description = "현장 아이디") @RequestParam("siteId", required = false) siteId: Long?,
    ): ResponseEntity<DataResponseBody<List<CctvResponse>>> = ResponseEntity.ok(DataResponseBody(cctvService.findAll(siteId)))

    @Operation(summary = "CCTV 상세 조회", description = "ID로 특정 CCTV의 상세 정보를 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "CCTV 조회 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "해당 ID의 CCTV를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/{id}")
    fun getById(
        @Parameter(description = "CCTV ID", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<CctvResponse>> = ResponseEntity.ok(DataResponseBody(cctvService.getById(id)))

    @Operation(summary = "CCTV 좌표 수정", description = "CCTV의 좌표 정보를 수정합니다. 좌표에 해당하는 현장이 자동 매핑됩니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "좌표 수정 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "해당 ID의 CCTV를 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @PatchMapping("/{id}/coordinates")
    fun updateCoordinates(
        @Parameter(description = "CCTV ID", required = true) @PathVariable id: Long,
        @Valid @RequestBody request: CctvCoordinateRequest,
    ): ResponseEntity<Void> {
        cctvService.updateCoordinates(id, request.lon, request.lat)
        return ResponseEntity.noContent().build()
    }
}
