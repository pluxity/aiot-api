package com.pluxity.aiot.site.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SiteRequest(
    @field:Schema(description = "현장 이름", example = "서울역", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "이름은 필수 입니다.")
    @field:Size(max = 50, message = "이름은 최대 50자까지 입력 가능합니다.")
    val name: String,
    @field:Schema(description = "geometry정보(WKT)", example = "POLYGON(..", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "location 정보는 필수 입니다.")
    val location: String,
    @field:Schema(description = "현장 설명", example = "description")
    @field:Size(max = 255, message = "현장 설명은 최대 255자까지 입력 가능합니다.")
    val description: String?,
)
