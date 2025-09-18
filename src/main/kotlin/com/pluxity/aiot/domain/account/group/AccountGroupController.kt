package com.pluxity.aiot.domain.account.group

import com.pluxity.aiot.domain.account.group.dto.AccountGroupRequest
import com.pluxity.aiot.domain.account.group.dto.AccountGroupResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/account-groups")
@Tag(name = "Account Group Controller", description = "사용자 그룹 관리 API")
class AccountGroupController(
    private val service: AccountGroupService,
) {
    @Operation(summary = "사용자 그룹 목록 조회", description = "모든 사용자 그룹 목록을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "목록 조회 성공",
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
    fun getAccounts(
        @Parameter(description = "검색어", example = "group")
        @RequestParam(required = false) search: String? = null,
    ): ResponseEntity<DataResponseBody<List<AccountGroupResponse>>> = ResponseEntity.ok(DataResponseBody(service.findAll(search)))

    @Operation(summary = "사용자 그룹 상세 조회", description = "ID로 특정 사용자 그룹의 상세 정보를 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
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
    @GetMapping("/{id}")
    fun getAccount(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<AccountGroupResponse>> = ResponseEntity.ok(DataResponseBody(service.find(id)))

    @Operation(summary = "사용자 그룹 생성", description = "새로운 사용자 그룹을 생성합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "사용자 그룹 생성 성공",
            ), ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
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
    @PostMapping
    @ResponseCreated(path = "/account-groups/{id}")
    fun postAccount(
        @Parameter(description = "사용자 그룹 생성 정보", required = true) @Valid @RequestBody dto: AccountGroupRequest,
    ): ResponseEntity<Long> = ResponseEntity.ok(service.save(dto))

    @Operation(summary = "사용자 그룹 수정", description = "기존 사용자 그룹의 정보를 수정합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "사용자 그룹 수정 성공",
            ), ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = ErrorResponseBody::class),
                    ),
                ],
            ), ApiResponse(
                responseCode = "404",
                description = "사용자 그룹을 찾을 수 없음",
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
    @PutMapping("/{id}")
    fun putAccount(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable id: Long,
        @Parameter(description = "사용자 그룹 수정 정보", required = true) @Valid @RequestBody dto: AccountGroupRequest,
    ): ResponseEntity<Void> {
        service.update(id, dto)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "사용자 그룹 삭제", description = "ID로 사용자 그룹을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "사용자 그룹 삭제 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "사용자 그룹을 찾을 수 없음",
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
    @DeleteMapping("/{id}")
    fun deleteAccount(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable id: Long,
    ): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "사용자 그룹 목록 삭제", description = "ID 목록에 해당하는 사용자 그룹을 삭제합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "사용자 그룹 삭제 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "사용자 그룹을 찾을 수 없음",
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
    @DeleteMapping("/ids/{ids}")
    fun deleteAccount(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable ids: List<Long>,
    ): ResponseEntity<List<Long>> {
        service.deleteAll(ids)
        return ResponseEntity.ok(ids)
    }

    // 권한 관련 API 엔드포인트 추가
    @Operation(summary = "사용자 그룹 접근 권한 조회", description = "ID에 해당하는 사용자 그룹의 접근권한을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "사용자 그룹을 찾을 수 없음",
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
    @GetMapping("/{id}/drawings")
    fun getAccessibleDrawings(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<List<Long>>> = ResponseEntity.ok(DataResponseBody(service.getAllAccessibleBoundaries(id)))

    @Operation(summary = "사용자 그룹 메뉴 권한 조회", description = "ID에 해당하는 사용자 그룹의 메뉴 권한을 조회합니다")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
            ), ApiResponse(
                responseCode = "404",
                description = "사용자 그룹을 찾을 수 없음",
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
    @GetMapping("/{id}/menus")
    fun getAccessibleMenus(
        @Parameter(description = "사용자 그룹 아이디", required = true) @PathVariable id: Long,
    ): ResponseEntity<DataResponseBody<List<String>>> = ResponseEntity.ok(DataResponseBody(service.getAllAccessibleMenuPaths(id)))
}
