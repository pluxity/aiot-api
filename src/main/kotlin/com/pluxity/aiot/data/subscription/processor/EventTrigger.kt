package com.pluxity.aiot.data.subscription.processor

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.EventCondition

/** 이벤트 발생에 필요한 판정 결과. 조건 테이블 없이 단말 값만으로 판정하는 센서도 같은 경로로 이벤트를 낸다 */
data class EventTrigger(
    val level: ConditionLevel,
    val guideMessage: String?,
    val notificationEnabled: Boolean,
    val minValue: Double,
    val maxValue: Double,
)

fun EventCondition.toEventTrigger() =
    EventTrigger(
        level = level,
        guideMessage = guideMessage,
        notificationEnabled = notificationEnabled,
        minValue = thresholdValue ?: leftValue ?: 0.0,
        maxValue = rightValue ?: 0.0,
    )
