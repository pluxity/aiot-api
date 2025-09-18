package com.pluxity.aiot.abbreviation.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class AbbreviationRequest(
    @field:NotNull(message = "분류는 필수 입니다.")
    @field:NotBlank(message = "분류는 공백이 될 수 없습니다.")
    @field:Schema(description = "분류", example = "디바이스명", requiredMode = Schema.RequiredMode.REQUIRED)
    var type: String,
    @field:NotNull(message = "약어는 필수 입니다.")
    @field:NotBlank(message = "약어는 공백이 될 수 없습니다.")
    @field:Schema(description = "약어", example = "THM", requiredMode = Schema.RequiredMode.REQUIRED)
    var abbreviationKey: String,
    @field:NotNull(message = "정식명칭은 필수 입니다.")
    @field:NotBlank(message = "정식명칭은 공백이 될 수 없습니다.")
    @field:Schema(description = "정식명칭", example = "온습도계", requiredMode = Schema.RequiredMode.REQUIRED)
    var fullName: String,
    @field:Schema(description = "설명", example = "description")
    var description: String? = null,
    @field:Schema(description = "적용여부")
    var isActive: Boolean = true,
)
