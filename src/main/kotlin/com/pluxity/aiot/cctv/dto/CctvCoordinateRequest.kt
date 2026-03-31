package com.pluxity.aiot.cctv.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "CCTV 좌표 수정 요청")
data class CctvCoordinateRequest(
    @field:Schema(description = "경도", example = "127.0")
    val lon: Double,
    @field:Schema(description = "위도", example = "37.0")
    val lat: Double,
)
