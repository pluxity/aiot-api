package com.pluxity.aiot.action

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse

data class ActionHistoryResponse(
    var id: Long? = null,
    var deviceId: String? = null,
    var eventName: String? = null,
    var eventHistoryId: Long? = null,
    var content: String? = null,
    var files: List<FileResponse>? = null,
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
        baseResponse = this.toBaseResponse(),
    )

data class ActionHistoryRequest(
    var fileIds: List<Long>?,
    var content: String? = null,
)
