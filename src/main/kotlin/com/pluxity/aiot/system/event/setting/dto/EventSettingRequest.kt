package com.pluxity.aiot.system.event.setting.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class EventSettingRequest(
    @field:NotNull(message = "이벤트 설정 아이디는 필수 입니다.")
    @field:Schema(description = "이벤트 설정 아이디", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    var id: Long,
    @field:NotNull(message = "디바이스 프로필 아이디는 필수 입니다.")
    @field:Schema(description = "디바이스 프로필 아이디", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    var deviceProfileTypeId: Long,
    var eventEnabled: Boolean = false,
    var conditions: List<EventConditionRequest>,
    var isPeriodic: Boolean,
    var months: List<Int>?,
)
