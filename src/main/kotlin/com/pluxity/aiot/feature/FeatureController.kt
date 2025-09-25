package com.pluxity.aiot.feature

import com.pluxity.aiot.data.AiotService
import com.pluxity.aiot.feature.dto.FeatureResponse
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/features")
@Tag(name = "Feature Controller", description = "피처 관리 API")
class FeatureController(
    private val featureService: FeatureService,
    private val aiotService: AiotService,
) {
    @Operation(
        summary = "피처 목록 조회",
        description = "모든 피처 목록을 조회합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
            ), ApiResponse(
                responseCode = "400",
                description = "파라미터 오류",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ), ApiResponse(
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
        @Parameter(description = "검색 조건") searchCondition: FeatureSearchCondition,
    ): ResponseEntity<DataResponseBody<List<FeatureResponse>>> =
        ResponseEntity.ok(DataResponseBody(featureService.findAll(searchCondition)))

    @Operation(summary = "피쳐 정보 수정", description = "피쳐 정보를 수정합니다")
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
                description = "피쳐를 찾을 수 없음",
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
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: FeatureUpdateRequest,
    ): ResponseEntity<Void> {
        featureService.updateFeature(id, request)
        return ResponseEntity.ok().build()
    }

    @Operation(
        summary = "피쳐 데이터 최신화",
        description = "피쳐 데이터를 최신화합니다",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "성공",
            ),
        ],
    )
    @PostMapping("/sync")
    fun checkSync(): ResponseEntity<Void> {
        aiotService.checkSynchronization()
        aiotService.statusSynchronize()
        aiotService.subscription()
        return ResponseEntity.noContent().build()
    }
}
