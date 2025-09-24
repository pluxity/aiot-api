package com.pluxity.aiot.action

import java.time.LocalDateTime

data class ActionHistoryResponse(
    var id: Long? = null,
    var deviceId: String? = null,
    var eventName: String? = null,
    var eventHistoryId: Long? = null,
    var actionType: String? = null,
    var actionResult: String? = null,
    var ignored: Boolean = false,
    var actedAt: LocalDateTime? = null,
    var actedBy: String? = null,
    var content: String? = null,
)

fun ActionHistory.toActionHistoryResponse(): ActionHistoryResponse =
    ActionHistoryResponse(
        id = this.id,
        deviceId = this.deviceId,
        eventName = this.eventName,
        eventHistoryId = this.eventHistory.id,
        actionType = this.actionType.name,
        actionResult = this.actionResult.name,
        ignored = this.ignored,
        actedAt = this.actedAt,
    )

data class ActionHistoryRequest(
    var deviceId: String,
    var eventName: String,
    var eventHistoryId: Long,
    var actionType: String,
    var actionResult: String,
    var ignored: Boolean = false,
    var actedBy: String? = null,
    var content: String? = null,
)
