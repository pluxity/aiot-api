package com.pluxity.aiot.site

import com.pluxity.aiot.global.annotation.ResponseCreated
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.site.dto.SiteRequest
import com.pluxity.aiot.site.dto.SiteResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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
@RequestMapping("/sites")
@Tag(name = "Site Controller", description = "현장 관리 API")
class SiteController(
    private val siteService: SiteService,
) {
    @Operation(summary = "현장 목록 조회", description = "모든 현장 목록을 조회합니다")
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
    fun getFacilities(): ResponseEntity<DataResponseBody<List<SiteResponse>>> = ResponseEntity.ok(DataResponseBody(siteService.findAll()))

    @Operation(summary = "현장 상세 조회", description = "특정 아이디 현장을 조회합니다")
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
    @GetMapping("/{siteId}")
    fun getSite(
        @Parameter(description = "현장 ID", required = true) @PathVariable siteId: Long,
    ): ResponseEntity<DataResponseBody<SiteResponse>> = ResponseEntity.ok(DataResponseBody(siteService.findByIdResponse(siteId)))

    @Operation(summary = "현장 정보 수정", description = "현장 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "정보 수정 성공"),
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
                description = "현장을 찾을 수 없음",
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
    @PutMapping("/{siteId}")
    fun patchLocation(
        @Parameter(description = "현장 ID", required = true) @PathVariable siteId: Long,
        @Parameter(description = "수정 정보", required = true) @Valid @RequestBody
        request: SiteRequest,
    ): ResponseEntity<Void> {
        siteService.putUpdate(siteId, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "현장 생성", description = "새로운 현장을 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "성공"),
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
    @ResponseCreated(path = "/facilities/{id}")
    @PostMapping
    fun save(
        @Parameter(description = "현장 정보", required = true) @Valid @RequestBody
        request: SiteRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(siteService.save(request))

    @Operation(summary = "현장 삭제", description = "현장을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
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
                description = "현장을 찾을 수 없음",
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
    @DeleteMapping("/{siteId}")
    fun deletePath(
        @Parameter(description = "현장 ID", required = true) @PathVariable siteId: Long,
    ): ResponseEntity<Void> {
        siteService.delete(siteId)
        return ResponseEntity.noContent().build()
    }
}
