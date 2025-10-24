package com.pluxity.aiot.system.event.condition

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate

@Entity
class EventCondition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "object_id", nullable = false)
    var objectId: String,
    @Column(name = "field_key", nullable = false)
    var fieldKey: String,
    @Column(name = "is_active")
    var isActivate: Boolean = false,
    var notificationEnabled: Boolean = false,
    @Column(name = "condition_order")
    var order: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var level: ConditionLevel,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var conditionType: ConditionType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var operator: Operator,
    @Column(name = "threshold_value")
    var thresholdValue: Double? = null,
    @Column(name = "left_value")
    var leftValue: Double? = null,
    @Column(name = "right_value")
    var rightValue: Double? = null,
    @Column(name = "boolean_value")
    var booleanValue: Boolean? = null,
) {
    init {
        if (order == null) {
            this.order = getDefaultOrderByDeviceLevel()
        }
        validate()
    }

    @PrePersist
    @PreUpdate
    fun validateBeforeSave() {
        validate()
    }

    private fun validate() {
        validateOrThrow()
    }

    fun getActualRange(): Pair<Double, Double>? {
        if (conditionType != ConditionType.RANGE) {
            return null
        }

        val left = leftValue ?: return null
        val right = rightValue ?: return null

        // DisplacementGauge인 경우: leftValue는 errorRange, rightValue는 centerValue
        return if (objectId == "34957") {
            val errorRange = left
            val centerValue = right
            val minRange = centerValue - errorRange
            val maxRange = centerValue + errorRange
            Pair(minRange, maxRange)
        } else {
            // 일반 센서인 경우: leftValue와 rightValue는 직접적인 범위
            Pair(left, right)
        }
    }

    /**
     * 두 조건의 범위가 겹치는지 확인합니다.
     */
    fun hasRangeOverlap(other: EventCondition): Boolean {
        if (this.objectId != other.objectId || this.fieldKey != other.fieldKey) {
            return false
        }

        // 같은 조건이면 스킵
        if (this.id != null && this.id == other.id) {
            return false
        }

        val thisRange = this.getActualRange() ?: return false
        val otherRange = other.getActualRange() ?: return false

        val (thisMin, thisMax) = thisRange
        val (otherMin, otherMax) = otherRange

        return !(thisMax < otherMin || otherMax < thisMin)
    }

    fun update(
        objectId: String,
        fieldKey: String,
        isActivate: Boolean,
        level: ConditionLevel,
        conditionType: ConditionType,
        operator: Operator,
        thresholdValue: Double?,
        leftValue: Double?,
        rightValue: Double?,
        booleanValue: Boolean?,
        notificationEnabled: Boolean,
        order: Int?,
    ) {
        this.objectId = objectId
        this.fieldKey = fieldKey
        this.isActivate = isActivate
        this.level = level
        this.conditionType = conditionType
        this.operator = operator
        this.thresholdValue = thresholdValue
        this.leftValue = leftValue
        this.rightValue = rightValue
        this.booleanValue = booleanValue
        this.notificationEnabled = notificationEnabled
        this.order = order ?: this.order

        validate()
    }

    private fun getDefaultOrderByDeviceLevel(): Int =
        when (this.level) {
            ConditionLevel.NORMAL -> 1
            ConditionLevel.WARNING -> 2
            ConditionLevel.CAUTION -> 3
            ConditionLevel.DANGER -> 4
            ConditionLevel.DISCONNECTED -> -1
        }
}

enum class ConditionLevel {
    NORMAL,
    WARNING,
    CAUTION,
    DANGER,
    DISCONNECTED,
}

enum class ConditionType {
    SINGLE,
    RANGE,
}

enum class Operator {
    GE,
    LE,
    BETWEEN,
}
