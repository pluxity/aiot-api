package com.pluxity.aiot.system.event.setting.dto


data class EventConditionRequest(
    val id: Long?,
    val isActivate: Boolean,
    val needControl: Boolean,
    val isBoolean: Boolean,
    val minValue: String?,
    val maxValue: String?,
    val notificationEnabled: Boolean,
    val notificationIntervalMinutes: Int?,
    val order: Int?,
)
