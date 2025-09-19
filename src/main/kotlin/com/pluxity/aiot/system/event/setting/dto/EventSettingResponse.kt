package com.pluxity.aiot.system.event.setting.dto

import com.pluxity.aiot.system.event.setting.EventSetting

class EventSettingResponse(
    val id: Long?,
    val deviceProfileTypeId: Long?,
    val eventEnabled: Boolean,
    val conditions: List<EventConditionResponse>?,
    val isPeriodic: Boolean,
    val months: List<Int>?,
    val isOriginal: Boolean,
)

fun EventSetting.toEventSettingResponse() =
    EventSettingResponse(
        id = this.id,
        deviceProfileTypeId = this.deviceProfileType?.id,
        eventEnabled = this.eventEnabled,
        conditions = this.conditions.map { it.toEventConditionResponse() }.toList(),
        isPeriodic = this.isPeriodic,
        months = this.months?.toList(),
        isOriginal = this.isOriginal,
    )
