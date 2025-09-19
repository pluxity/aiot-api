package com.pluxity.aiot.system.device.profile.dto

import com.pluxity.aiot.system.device.profile.DeviceProfile
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class DeviceProfileRequest(
    @field:NotNull(message = "필드명은 필수 입니다.")
    @field:NotBlank(message = "필드명은 공백이 될 수 없습니다.")
    @field:Schema(description = "필드명", example = "Temperature", requiredMode = Schema.RequiredMode.REQUIRED)
    var fieldKey: String,
    @field:NotNull(message = "용도는 필수 입니다.")
    @field:NotBlank(message = "용도는 공백이 될 수 없습니다.")
    @field:Schema(description = "용도", example = "온도", requiredMode = Schema.RequiredMode.REQUIRED)
    var description: String,
    @field:Schema(description = "단위", example = "°C")
    var fieldUnit: String?,
    @field:NotNull(message = "자료형은 필수 입니다.")
    @field:Schema(description = "자료형", requiredMode = Schema.RequiredMode.REQUIRED)
    var fieldType: DeviceProfile.FieldType,
)
