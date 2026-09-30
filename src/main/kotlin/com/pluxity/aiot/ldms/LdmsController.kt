package com.pluxity.aiot.ldms

import com.pluxity.aiot.global.response.DataResponseBody
import com.pluxity.aiot.global.response.ErrorResponseBody
import com.pluxity.aiot.ldms.dto.LdmsDisplayResponse
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ldms")
@Tag(name = "LDMS Controller", description = "LDMS 연동 전광판 · 송출그룹 API")
@ConditionalOnProperty("ldms.enabled", havingValue = "true")
class LdmsController(
    private val ldmsService: LdmsService,
) {
    @Operation(summary = "LDMS 전광판 목록 조회", description = "LDMS 서버에 등록된 전체 전광판 목록을 실시간으로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            ApiResponse(
                responseCode = "502",
                description = "LDMS API 호출 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "LDMS 로그인 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/displays")
    fun getDisplays(): ResponseEntity<DataResponseBody<List<LdmsDisplayResponse>>> =
        ResponseEntity.ok(DataResponseBody(ldmsService.findDisplays()))

    @Operation(summary = "LDMS 송출그룹 목록 조회", description = "LDMS 서버에 등록된 송출그룹 목록을 실시간으로 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            ApiResponse(
                responseCode = "502",
                description = "LDMS API 호출 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
            ApiResponse(
                responseCode = "503",
                description = "LDMS 로그인 실패",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseBody::class))],
            ),
        ],
    )
    @GetMapping("/emcall-groups")
    fun getEmcallGroups(): ResponseEntity<DataResponseBody<List<LdmsEmcallGroupResponse>>> =
        ResponseEntity.ok(DataResponseBody(ldmsService.findEmcallGroups()))
}
