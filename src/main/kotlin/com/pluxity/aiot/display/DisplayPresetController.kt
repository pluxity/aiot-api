package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayPresetRequest
import com.pluxity.aiot.display.dto.DisplayPresetResponse
import com.pluxity.aiot.display.dto.DisplayPresetSearchRequest
import com.pluxity.aiot.global.annotation.ResponseCreated
import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.global.response.PageResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/display-presets")
@Tag(name = "Display Preset Controller", description = "전광판(LED) 메시지 프리셋 API")
class DisplayPresetController(
    private val displayPresetService: DisplayPresetService,
) {
    @Operation(summary = "전광판 프리셋 목록 조회", description = "전광판 메시지 프리셋 목록을 페이징 조회합니다. 제목으로 검색할 수 있습니다.")
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
    fun getPresets(
        @Parameter(description = "조회 페이지번호", example = "1")
        @RequestParam("page", required = false) page: Int = 1,
        @Parameter(description = "페이지당 개수", example = "10")
        @RequestParam("size", required = false) size: Int = 10,
        @Parameter(description = "제목 검색어", example = "폐장")
        @RequestParam("title", required = false) title: String? = null,
    ): ResponseEntity<DataResponseBody<PageResponse<DisplayPresetResponse>>> =
        ResponseEntity.ok(DataResponseBody(displayPresetService.findAll(DisplayPresetSearchRequest(page, size, title))))

    @Operation(summary = "전광판 프리셋 상세 조회", description = "아이디로 전광판 메시지 프리셋을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "프리셋을 찾을 수 없음",
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
    @GetMapping("/{presetId}")
    fun getPreset(
        @Parameter(description = "프리셋 아이디", required = true) @PathVariable presetId: Long,
    ): ResponseEntity<DataResponseBody<DisplayPresetResponse>> =
        ResponseEntity.ok(DataResponseBody(displayPresetService.findById(presetId)))

    @Operation(summary = "전광판 프리셋 생성", description = "새로운 전광판 메시지 프리셋을 생성합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "생성 성공"),
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
    @ResponseCreated(path = "/display-presets/{id}")
    @PostMapping
    fun save(
        @Parameter(description = "프리셋 정보", required = true) @Valid @RequestBody
        request: DisplayPresetRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(displayPresetService.save(request))

    @Operation(summary = "전광판 프리셋 수정", description = "전광판 메시지 프리셋을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "수정 성공"),
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
                description = "프리셋을 찾을 수 없음",
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
    @PutMapping("/{presetId}")
    fun update(
        @Parameter(description = "프리셋 아이디", required = true) @PathVariable presetId: Long,
        @Parameter(description = "수정 정보", required = true) @Valid @RequestBody
        request: DisplayPresetRequest,
    ): ResponseEntity<Void> {
        displayPresetService.update(presetId, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "전광판 프리셋 삭제", description = "전광판 메시지 프리셋을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "프리셋을 찾을 수 없음",
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
    @DeleteMapping("/{presetId}")
    fun delete(
        @Parameter(description = "프리셋 아이디", required = true) @PathVariable presetId: Long,
    ): ResponseEntity<Void> {
        displayPresetService.delete(presetId)
        return ResponseEntity.noContent().build()
    }
}
