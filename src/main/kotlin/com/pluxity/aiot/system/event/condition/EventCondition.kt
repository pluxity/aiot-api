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
    @Column(name = "is_active")
    var isActivate: Boolean = false,
    var notificationEnabled: Boolean = false,
    @Column(name = "condition_order")
    var order: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var level: ConditionLevel, // 이벤트 상태 (예: CRITICAL, NORMAL)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var dataType: DataType, // 데이터 타입 정의 (NUMERIC, BOOLEAN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var operator: Operator, // 어떤 연산을 수행할 것인지 (GT, GOE, LT, LOE, BETWEEN, EQ, NE)
    @Column(name = "numeric_value1")
    var numericValue1: Double? = null, // NUMERIC 타입일 때 사용. Integer, Float, Double 모두 Double로 저장 가능.
    @Column(name = "numeric_value2")
    var numericValue2: Double? = null, // NUMERIC 이고 operator가 BETWEEN일 때만 사용.
    @Column(name = "boolean_value")
    var booleanValue: Boolean? = null, // BOOLEAN 타입일 때 사용.
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
        when (dataType) {
            DataType.NUMERIC -> {
                require(numericValue1 != null) {
                    "NUMERIC 타입은 numericValue1이 필수입니다"
                }
                require(booleanValue == null) {
                    "NUMERIC 타입에서는 booleanValue를 사용할 수 없습니다"
                }

                when (operator) {
                    Operator.BETWEEN -> {
                        require(numericValue2 != null) {
                            "BETWEEN 연산자는 numericValue2가 필수입니다"
                        }
                    }
                    Operator.GREATER_THAN,
                    Operator.GREATER_OR_EQUAL,
                    Operator.LESS_THAN,
                    Operator.LESS_OR_EQUAL,
                    -> {
                        require(numericValue2 == null) {
                            "$operator 연산자는 numericValue2를 사용할 수 없습니다"
                        }
                    }
                    Operator.EQUAL,
                    Operator.NOT_EQUAL,
                    -> {
                        require(numericValue2 == null) {
                            "$operator 연산자는 numericValue2를 사용할 수 없습니다"
                        }
                    }
                }
            }

            DataType.BOOLEAN -> {
                require(booleanValue != null) {
                    "BOOLEAN 타입은 booleanValue가 필수입니다"
                }
                require(numericValue1 == null && numericValue2 == null) {
                    "BOOLEAN 타입에서는 numericValue를 사용할 수 없습니다"
                }
                require(operator in listOf(Operator.EQUAL, Operator.NOT_EQUAL)) {
                    "BOOLEAN 타입에서는 EQUAL, NOT_EQUAL만 사용 가능합니다 (현재: $operator)"
                }
            }
        }
    }

    fun update(
        objectId: String,
        isActivate: Boolean,
        level: ConditionLevel,
        dataType: DataType,
        operator: Operator,
        numericValue1: Double?,
        numericValue2: Double?,
        booleanValue: Boolean?,
        notificationEnabled: Boolean,
        order: Int?,
    ) {
        this.objectId = objectId
        this.isActivate = isActivate
        this.level = level
        this.dataType = dataType
        this.operator = operator
        this.numericValue1 = numericValue1
        this.numericValue2 = numericValue2
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

enum class Operator {
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    BETWEEN,
    EQUAL,
    NOT_EQUAL,
}

enum class DataType {
    NUMERIC,
    BOOLEAN,
}
