package com.pluxity.aiot.action

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ActionHistoryResponse(
    var id: Long? = null,
    var deviceId: String,
    var eventName: String? = null,
    var eventHistoryId: Long? = null,
    var content: String,
    var files: List<FileResponse>? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    @field:JsonUnwrapped var baseResponse: BaseResponse? = null,
)

fun ActionHistory.toActionHistoryResponse(fileMap: Map<Long, FileResponse>): ActionHistoryResponse =
    ActionHistoryResponse(
        id = this.id,
        deviceId = this.eventHistory.deviceId,
        eventName = this.eventHistory.eventName,
        eventHistoryId = this.eventHistory.id,
        content = this.content,
        files = this.historyFiles.mapNotNull { fileMap[it.fileId] },
        longitude = this.eventHistory.longitude,
        latitude = this.eventHistory.latitude,
        baseResponse = this.toBaseResponse(),
    )

data class ActionHistoryRequest(
    @field:Schema(description = "조치 내역", example = "조치완료함")
    @field:NotBlank(message = "조치 내역은 필수입니다")
    var content: String,
    @field:Schema(description = "파일 ID", example = "1")
    var fileIds: List<Long>? = null,
)
