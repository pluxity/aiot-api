package com.pluxity.aiot.feature.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FeatureUpdateRequest(
    @field:Schema(description = "디바이스 종류 아이디")
    val deviceTypeId: Long,
    @field:Schema(description = "활성화여부")
    val isActive: Boolean,
)
