package com.pluxity.aiot.site.dto

import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteSensorManager
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "센서 카테고리 담당자 지정 요청. 지정된 목록으로 전체 교체하며, 빈 배열이면 전원 해제된다")
data class SiteSensorManagerRequest(
    @field:Schema(description = "담당자로 지정할 사용자 ID 목록", example = "[1, 2]")
    val userIds: List<Long> = emptyList(),
)

@Schema(description = "센서 카테고리별 담당자")
data class SiteSensorManagerResponse(
    val sensorType: SensorType,
    @field:Schema(description = "센서 카테고리 설명", example = "화재감지기")
    val sensorTypeDescription: String,
    val managers: List<SiteManagerResponse>,
)

@Schema(description = "담당자")
data class SiteManagerResponse(
    val userId: Long,
    val name: String,
    val phoneNumber: String?,
    val department: String?,
)

fun SiteSensorManager.toManagerResponse() =
    SiteManagerResponse(
        userId = user.requiredId,
        name = user.name,
        phoneNumber = user.phoneNumber,
        department = user.department,
    )
