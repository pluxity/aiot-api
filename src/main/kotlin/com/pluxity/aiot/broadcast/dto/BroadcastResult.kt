package com.pluxity.aiot.broadcast.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "송출 대상별 결과")
data class BroadcastResult(
    @field:Schema(description = "송출 대상 장치 아이디", example = "1")
    val targetId: Long,
    @field:Schema(description = "송출 대상 장치 명칭", example = "정문 스피커")
    val targetName: String,
    @field:Schema(description = "송출 대상 장치의 소속 현장 아이디", example = "1")
    val siteId: Long?,
    @field:Schema(description = "송출 대상 장치의 소속 현장 명칭", example = "중앙공원")
    val siteName: String?,
    @field:Schema(description = "송출 성공 여부", example = "true")
    val success: Boolean,
    @field:Schema(description = "송출 실패 사유. 성공 시 null", example = "장치가 응답하지 않습니다.")
    val failureReason: String? = null,
)
