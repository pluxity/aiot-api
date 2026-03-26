package com.pluxity.aiot.eds

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/cctvs")
@Tag(name = "CCTV Controller", description = "CCTV 관리 API")
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsController(
    private val edsCameraSyncService: EdsCameraSyncService,
) {
    @Operation(summary = "EDS 카메라 동기화", description = "EDS 서버의 카메라 목록을 조회하여 CCTV 테이블과 동기화합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "EDS 카메라 동기화 성공",
            ),
        ],
    )
    @PostMapping("/sync")
    fun sync(): ResponseEntity<Void> {
        edsCameraSyncService.sync()
        return ResponseEntity.noContent().build()
    }
}
