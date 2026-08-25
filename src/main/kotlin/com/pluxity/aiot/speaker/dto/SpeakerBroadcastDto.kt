package com.pluxity.aiot.speaker.dto

import com.fasterxml.jackson.annotation.JsonIgnore
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

@Schema(
    description =
        "스피커 송출 이력. 업체 API 가 장치 단건 처리이므로 이력도 장치 1건 단위로 남습니다. " +
            "한 번의 송출 요청이 스피커 N개를 대상으로 하면 이력 N건이 생성됩니다",
)
data class SpeakerBroadcastResponse(
    @field:Schema(description = "송출 이력 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "송출 시점의 메시지 스냅샷", example = "잠시 후 공원이 폐장합니다.")
    val message: String,
    @field:Schema(description = "송출 대상 스피커 아이디", example = "1")
    val speakerId: Long,
    @field:Schema(description = "송출 대상 스피커 명칭", example = "정문 스피커")
    val speakerName: String,
    @field:Schema(description = "송출 대상 스피커의 소속 현장 아이디", example = "1")
    val siteId: Long?,
    @field:Schema(description = "송출 대상 스피커의 소속 현장 명칭", example = "중앙공원")
    val siteName: String?,
    @field:Schema(description = "송출자 계정 아이디", example = "admin")
    val userId: String,
    @field:Schema(description = "송출자 이름", example = "홍길동")
    val userName: String,
    @field:Schema(description = "송출 시각", example = "2026-08-25T09:00:00")
    val broadcastAt: String,
    @field:Schema(description = "송출 성공 여부", example = "true")
    val success: Boolean,
    @field:Schema(description = "송출 실패 사유. 성공 시 null", example = "장치가 응답하지 않습니다.")
    val failureReason: String?,
)
