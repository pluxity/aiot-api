package com.pluxity.aiot.abbreviation

import com.pluxity.aiot.abbreviation.dto.AbbreviationRequest
import com.pluxity.aiot.abbreviation.dto.AbbreviationResponse
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
@RequestMapping("/abbreviations")
@Tag(name = "Abbreviation Controller", description = "약어 관리 API")
class AbbreviationController(
    private val abbreviationService: AbbreviationService,
) {
    @Operation(summary = "약어 목록 조회", description = "모든 약어 목록을 조회합니다")
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
    fun getAllAbbreviations(): ResponseEntity<DataResponseBody<List<AbbreviationResponse>>> =
        ResponseEntity.ok(DataResponseBody(abbreviationService.getAllAbbreviations()))

    @Operation(summary = "약어 상세 조회", description = "특정 아이디 약어를 조회합니다")
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
    @GetMapping("/{abbreviationId}")
    fun getAbbreviationById(
        @Parameter(description = "약어 ID", required = true) @PathVariable abbreviationId: Long,
    ): ResponseEntity<DataResponseBody<AbbreviationResponse>> =
        ResponseEntity.ok(DataResponseBody(abbreviationService.getAbbreviationById(abbreviationId)))

    @Operation(summary = "약어 생성", description = "새로운 약어를 생성합니다")
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
    @ResponseCreated(path = "/abbreviations/{abbreviationId}")
    @PostMapping
    fun createAbbreviation(
        @RequestBody @Valid request: AbbreviationRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(abbreviationService.createAbbreviation(request))

    @Operation(summary = "약어 정보 수정", description = "약어 정보를 수정합니다")
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
                description = "약어를 찾을 수 없음",
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
    @PutMapping("/{abbreviationId}")
    fun updateAbbreviation(
        @Parameter(description = "약어 ID", required = true) @PathVariable abbreviationId: Long,
        @RequestBody @Valid request: AbbreviationRequest,
    ): ResponseEntity<Void> {
        abbreviationService.updateAbbreviation(abbreviationId, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "약어 삭제", description = "약어를 삭제합니다")
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
                description = "약어를 찾을 수 없음",
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
    @DeleteMapping("/{abbreviationId}")
    fun deleteAbbreviation(
        @Parameter(description = "약어 ID", required = true) @PathVariable abbreviationId: Long,
    ): ResponseEntity<Void> {
        abbreviationService.deleteAbbreviation(abbreviationId)
        return ResponseEntity.noContent().build()
    }
}
