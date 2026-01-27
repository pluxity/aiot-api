package com.pluxity.aiot.mobius.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

class MobiusRequest(
    @field:NotBlank(message = "url은 공백이 될 수 없습니다.")
    @field:Schema(description = "url", example = "http://192.168.0.1", requiredMode = Schema.RequiredMode.REQUIRED)
    val url: String,
)
