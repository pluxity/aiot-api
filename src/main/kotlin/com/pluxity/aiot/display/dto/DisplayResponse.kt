package com.pluxity.aiot.display.dto

import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.site.dto.SiteResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전광판 응답")
data class DisplayResponse(
    @field:Schema(description = "전광판 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "전광판 명칭", example = "정문 전광판")
    val name: String,
    @field:Schema(description = "업체 장비 식별자. 송출 시 업체 API 로 전달합니다", example = "LED-0001")
    val deviceId: String,
    @field:Schema(description = "설치 위치", example = "정문 매표소 상단")
    val location: String?,
    @field:Schema(description = "장치 상태")
    val status: DeviceStatus,
    @field:Schema(description = "소속 현장")
    val site: SiteResponse?,
)
