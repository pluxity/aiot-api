package com.pluxity.aiot.speaker.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.pluxity.aiot.broadcast.dto.BroadcastResult
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size

@Schema(
    description =
        "스피커 송출 요청. presetId 와 message 중 정확히 하나, " +
            "speakerIds 와 siteIds 중 하나 이상을 지정합니다",
)
data class SpeakerBroadcastRequest(
    @field:Schema(description = "송출할 프리셋 아이디. message 와 둘 중 하나만 지정합니다", example = "1")
    val presetId: Long? = null,
    @field:Schema(
        description = "직접 입력한 메시지. presetId 와 둘 중 하나만 지정합니다",
        example = "잠시 후 공원이 폐장합니다.",
    )
    @field:Size(max = 1000, message = "메시지는 최대 1000자까지 입력 가능합니다.")
    val message: String? = null,
    @field:Schema(description = "송출 대상 스피커 아이디 목록", example = "[1, 2]")
    val speakerIds: List<Long>? = null,
    @field:Schema(description = "송출 대상 현장 아이디 목록. 해당 현장의 모든 스피커로 송출합니다", example = "[1]")
    val siteIds: List<Long>? = null,
) {
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "프리셋 아이디와 메시지 중 정확히 하나를 지정해야 합니다.")
    fun isContentValid(): Boolean = (presetId != null) xor !message.isNullOrBlank()

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "스피커 아이디 또는 현장 아이디 중 하나 이상은 필수 입니다.")
    fun isTargetValid(): Boolean = !speakerIds.isNullOrEmpty() || !siteIds.isNullOrEmpty()
}

data class SpeakerBroadcastSearchRequest(
    val page: Int = 1,
    val size: Int = 10,
    val from: String? = null,
    val to: String? = null,
    val userId: String? = null,
    val siteId: Long? = null,
)

@Schema(description = "스피커 송출 이력 목록 항목")
data class SpeakerBroadcastSummaryResponse(
    @field:Schema(description = "송출 이력 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "실제 송출된 메시지", example = "잠시 후 공원이 폐장합니다.")
    val message: String,
    @field:Schema(description = "프리셋 송출인 경우 프리셋 아이디. 직접 입력이면 null", example = "1")
    val presetId: Long?,
    @field:Schema(description = "프리셋 송출인 경우 프리셋 제목. 직접 입력이면 null", example = "폐장 안내")
    val presetTitle: String?,
    @field:Schema(description = "송출자 아이디", example = "admin")
    val userId: String,
    @field:Schema(description = "송출 시각", example = "2026-08-25T09:00:00")
    val broadcastAt: String,
    @field:Schema(description = "송출 대상 총 개수", example = "2")
    val totalCount: Int,
    @field:Schema(description = "송출 성공 개수", example = "1")
    val successCount: Int,
    @field:Schema(description = "송출 실패 개수", example = "1")
    val failureCount: Int,
)

@Schema(description = "스피커 송출 이력 상세. 대상별 성공/실패 사유를 포함합니다")
data class SpeakerBroadcastResponse(
    @field:Schema(description = "송출 이력 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "실제 송출된 메시지", example = "잠시 후 공원이 폐장합니다.")
    val message: String,
    @field:Schema(description = "프리셋 송출인 경우 프리셋 아이디. 직접 입력이면 null", example = "1")
    val presetId: Long?,
    @field:Schema(description = "프리셋 송출인 경우 프리셋 제목. 직접 입력이면 null", example = "폐장 안내")
    val presetTitle: String?,
    @field:Schema(description = "송출자 아이디", example = "admin")
    val userId: String,
    @field:Schema(description = "송출 시각", example = "2026-08-25T09:00:00")
    val broadcastAt: String,
    @field:Schema(description = "송출 대상 총 개수", example = "2")
    val totalCount: Int,
    @field:Schema(description = "송출 성공 개수", example = "1")
    val successCount: Int,
    @field:Schema(description = "송출 실패 개수", example = "1")
    val failureCount: Int,
    @field:Schema(description = "송출 대상별 결과")
    val results: List<BroadcastResult>,
)
