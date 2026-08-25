package com.pluxity.aiot.display.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.global.response.BaseResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "전광판 프리셋 생성/수정 요청")
data class DisplayPresetRequest(
    @field:Schema(description = "프리셋 제목", example = "폐장 안내", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank(message = "제목은 필수 입니다.")
    @field:Size(max = 50, message = "제목은 최대 50자까지 입력 가능합니다.")
    val title: String,
    @field:Schema(
        description = "메시지 본문",
        example = "잠시 후 공원이 폐장합니다.",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "메시지는 필수 입니다.")
    @field:Size(max = 1000, message = "메시지는 최대 1000자까지 입력 가능합니다.")
    val message: String,
)

@Schema(description = "전광판 프리셋 응답")
data class DisplayPresetResponse(
    @field:Schema(description = "프리셋 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "프리셋 제목", example = "폐장 안내")
    val title: String,
    @field:Schema(description = "메시지 본문", example = "잠시 후 공원이 폐장합니다.")
    val message: String,
    @field:JsonUnwrapped val baseResponse: BaseResponse,
)

data class DisplayPresetSearchRequest(
    val page: Int = 1,
    val size: Int = 10,
    val title: String? = null,
)
