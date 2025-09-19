package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.EventSettingHistory
import java.time.LocalDateTime

data class EventSettingHistoryResponse(
    val id: Long?,
    val updatedAt: LocalDateTime?,
    val conditionHistories: List<ConditionHistoryValue>,
)

data class ConditionHistoryValue(
    val deviceEventId: Long?,
    val deviceEventName: String?,
    val value: String?,
    val operator: EventCondition.ConditionOperator?,
    val minValue: Double?,
    val maxValue: Double?,
    val notificationEnabled: Boolean,
    val locationTrackingEnabled: Boolean,
    val soundEnabled: Boolean,
    val controlType: EventCondition.ControlType?,
    val guideMessage: String?,
    val notificationIntervalMinutes: Int?,
)

fun EventSettingHistory.toEventSettingHistoryResponse(): EventSettingHistoryResponse =
    EventSettingHistoryResponse(
        id = this.id,
        updatedAt = this.updatedAt,
        conditionHistories = this.conditionInfos.map { it.toConditionHistoryValue() },
    )

fun EventSettingHistory.ConditionInfo.toConditionHistoryValue() =
    ConditionHistoryValue(
        deviceEventId = this.deviceEventId,
        deviceEventName = this.deviceEventName,
        value = this.value,
        operator = this.operator,
        minValue = this.minValue,
        maxValue = this.maxValue,
        notificationEnabled = this.notificationEnabled,
        locationTrackingEnabled = this.locationTrackingEnabled,
        soundEnabled = this.soundEnabled,
        controlType = this.controlType,
        guideMessage = this.guideMessage,
        notificationIntervalMinutes = this.notificationIntervalMinutes,
    )
