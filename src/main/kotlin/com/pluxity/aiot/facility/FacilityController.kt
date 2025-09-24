package com.pluxity.aiot.facility

import com.pluxity.aiot.facility.dto.FacilityRequest
import com.pluxity.aiot.facility.dto.FacilityResponse
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
@RequestMapping("/facilities")
@Tag(name = "Facility Controller", description = "시설 관리 API")
class FacilityController(
    private val facilityService: FacilityService,
) {
    @Operation(summary = "시설 목록 조회", description = "모든 시설 목록을 조회합니다")
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
    fun getFacilities(): ResponseEntity<DataResponseBody<List<FacilityResponse>>> =
        ResponseEntity.ok(DataResponseBody(facilityService.findAll()))

    @Operation(summary = "시설 상세 조회", description = "특정 아이디 시설을 조회합니다")
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
    @GetMapping("/{facilityId}")
    fun getFacility(
        @Parameter(description = "시설 ID", required = true) @PathVariable facilityId: Long,
    ): ResponseEntity<DataResponseBody<FacilityResponse>> =
        ResponseEntity.ok(DataResponseBody(facilityService.findByIdResponse(facilityId)))

    @Operation(summary = "시설 정보 수정", description = "시설 정보를 수정합니다")
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
                description = "시설을 찾을 수 없음",
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
    @PutMapping("/{facilityId}")
    fun patchLocation(
        @Parameter(description = "시설 ID", required = true) @PathVariable facilityId: Long,
        @Parameter(description = "수정 정보", required = true) @Valid @RequestBody
        request: FacilityRequest,
    ): ResponseEntity<Void> {
        facilityService.putUpdate(facilityId, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "시설 생성", description = "새로운 시설을 생성합니다")
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
        @Parameter(description = "시설 정보", required = true) @Valid @RequestBody
        request: FacilityRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(facilityService.save(request))

    @Operation(summary = "시설 삭제", description = "시설을 삭제합니다")
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
                description = "시설을 찾을 수 없음",
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
    @DeleteMapping("/{facilityId}")
    fun deletePath(
        @Parameter(description = "시설 ID", required = true) @PathVariable facilityId: Long,
    ): ResponseEntity<Void> {
        facilityService.deleteFacility(facilityId)
        return ResponseEntity.noContent().build()
    }
}
