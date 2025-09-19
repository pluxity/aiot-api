package com.pluxity.aiot.system.mobius.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

class MobiusRequest(
    @field:NotNull(message = "url은 필수 입니다.")
    @field:NotBlank(message = "url은 공백이 될 수 없습니다.")
    @field:Schema(description = "url", example = "http://192.168.0.1", requiredMode = Schema.RequiredMode.REQUIRED)
    var url: String,
)
