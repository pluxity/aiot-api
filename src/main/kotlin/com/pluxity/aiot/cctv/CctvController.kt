package com.pluxity.aiot.cctv

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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
}
