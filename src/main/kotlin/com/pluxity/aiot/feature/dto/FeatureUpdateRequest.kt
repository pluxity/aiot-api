package com.pluxity.aiot.feature.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FeatureUpdateRequest(
    @field:Schema(description = "활성화여부")
    val active: Boolean,
    @field:Schema(description = "고도")
    val height: Double?,
)
