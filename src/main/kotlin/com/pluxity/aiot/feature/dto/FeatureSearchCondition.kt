package com.pluxity.aiot.feature.dto

import io.swagger.v3.oas.annotations.media.Schema

data class FeatureSearchCondition(
    @field:Schema(description = "현장 아이디")
    var siteId: Long? = null,
    @field:Schema(description = "디바이스 object 아이디")
    var objectId: String? = null,
    @field:Schema(description = "이름")
    var name: String? = null,
    @field:Schema(description = "디바이스 아이디")
    var deviceId: String? = null,
    @field:Schema(description = "활성화여부")
    var isActive: Boolean? = null,
)
