package com.pluxity.aiot.event.condition.dto

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.Operator

data class EventConditionBatchRequest(
    val objectId: String,
    val conditions: List<EventConditionItemRequest>,
)

data class EventConditionItemRequest(
    val fieldKey: String, // DeviceProfile에 있는 Temperature 등
    val activate: Boolean, // 이벤트 활성화 여부
    val level: ConditionLevel, // 이벤트 레벨 (정상, 위험, 주의등)
    val conditionType: ConditionType?, // SINGLE, RANGE
    val operator: Operator?, // GREATER_OR_EQUAL, LESS_OR_EQUAL, BETWEEN
    val thresholdValue: Double?, // 단일 값
    val leftValue: Double?, // 왼쪽 값
    val rightValue: Double?, // 오른쪽 값
    val booleanValue: Boolean?, // Boolean 값
    val notificationEnabled: Boolean, // 알림 활성화 여부
    val guideMessage: String?, // 알림 메시지
)
