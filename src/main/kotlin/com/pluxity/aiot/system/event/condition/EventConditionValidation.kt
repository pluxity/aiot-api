package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.sensor.type.SensorType


sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(error: String) : this(listOf(error))
    }

    infix fun and(other: ValidationResult): ValidationResult =
        when (this) {
            is Valid if other is Valid -> Valid
            is Invalid if other is Invalid -> Invalid(this.errors + other.errors)
            is Invalid -> this
            else -> other
        }

    fun orThrow(): Unit =
        when (this) {
            is Valid -> Unit
            is Invalid -> throw IllegalArgumentException(errors.joinToString("; "))
        }
}

typealias Validator<T> = (T) -> ValidationResult

object EventConditionValidators {

    val validateObjectId: Validator<EventCondition> = { condition ->
        runCatching { SensorType.fromObjectId(condition.objectId) }
            .fold(
                onSuccess = { ValidationResult.Valid },
                onFailure = { ValidationResult.Invalid("objectId '${condition.objectId}'에 해당하는 SensorType을 찾을 수 없습니다") }
            )
    }

    val validateFieldKey: Validator<EventCondition> = { condition ->
        runCatching { SensorType.fromObjectId(condition.objectId) }
            .fold(
                onSuccess = { sensorType ->
                    val validFieldKeys = sensorType.deviceProfiles.map { it.fieldKey }
                    if (condition.fieldKey in validFieldKeys) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid(
                            "fieldKey '${condition.fieldKey}'는 SensorType '${sensorType.description}' " +
                                    "(objectId: ${condition.objectId})에서 사용할 수 없습니다. 유효한 값: $validFieldKeys"
                        )
                    }
                },
                onFailure = { ValidationResult.Valid } // objectId 검증은 validateObjectId에서 수행
            )
    }

    val validateBooleanType: Validator<EventCondition> = { condition ->
        condition.booleanValue?.let {
            listOf(
                Pair(condition.thresholdValue == null, "Boolean 조건에서는 thresholdValue를 사용할 수 없습니다"),
                Pair(condition.leftValue == null, "Boolean 조건에서는 leftValue를 사용할 수 없습니다"),
                Pair(condition.rightValue == null, "Boolean 조건에서는 rightValue를 사용할 수 없습니다")
            ).filterNot { (isValid, _) -> isValid }
                .map { (_, error) -> error }
                .let { errors ->
                    if (errors.isEmpty()) ValidationResult.Valid
                    else ValidationResult.Invalid(errors)
                }
        } ?: ValidationResult.Valid
    }

    val validateSingleType: Validator<EventCondition> = { condition ->
        if (condition.conditionType == ConditionType.SINGLE && condition.booleanValue == null) {
            listOf(
                Pair(
                    condition.operator in listOf(Operator.GOE, Operator.LOE),
                    "SINGLE 타입은 GOE 또는 LOE 연산자만 사용 가능합니다 (현재: ${condition.operator})"
                ),
                Pair(
                    condition.thresholdValue != null,
                    "SINGLE 타입은 thresholdValue가 필수입니다"
                ),
                Pair(
                    condition.leftValue == null,
                    "SINGLE 타입에서는 leftValue를 사용할 수 없습니다"
                ),
                Pair(
                    condition.rightValue == null,
                    "SINGLE 타입에서는 rightValue를 사용할 수 없습니다"
                )
            ).filterNot { (isValid, _) -> isValid }
                .map { (_, error) -> error }
                .let { errors ->
                    if (errors.isEmpty()) ValidationResult.Valid
                    else ValidationResult.Invalid(errors)
                }
        } else {
            ValidationResult.Valid
        }
    }

    val validateRangeType: Validator<EventCondition> = { condition ->
        if (condition.conditionType == ConditionType.RANGE && condition.booleanValue == null) {
            listOf(
                Pair(
                    condition.operator == Operator.BETWEEN,
                    "RANGE 타입은 BETWEEN 연산자만 사용 가능합니다 (현재: ${condition.operator})"
                ),
                Pair(
                    condition.leftValue != null,
                    "RANGE 타입은 leftValue가 필수입니다"
                ),
                Pair(
                    condition.rightValue != null,
                    "RANGE 타입은 rightValue가 필수입니다"
                ),
                Pair(
                    condition.thresholdValue == null,
                    "RANGE 타입에서는 thresholdValue를 사용할 수 없습니다"
                )
            ).filterNot { (isValid, _) -> isValid }
                .map { (_, error) -> error }
                .let { errors ->
                    if (errors.isEmpty()) ValidationResult.Valid
                    else ValidationResult.Invalid(errors)
                }
        } else {
            ValidationResult.Valid
        }
    }

    val validateDisplacementGauge: Validator<EventCondition> = { condition ->
        if (condition.objectId == "34957" && condition.conditionType == ConditionType.RANGE) {
            condition.leftValue?.let { errorRange ->
                condition.rightValue?.let { _ ->
                    if (errorRange > 0) {
                        ValidationResult.Valid
                    } else {
                        ValidationResult.Invalid("DisplacementGauge의 errorRange(leftValue)는 0보다 커야 합니다 (현재: $errorRange)")
                    }
                } ?: ValidationResult.Invalid("DisplacementGauge는 centerValue(rightValue)가 필수입니다")
            } ?: ValidationResult.Invalid("DisplacementGauge는 errorRange(leftValue)가 필수입니다")
        } else {
            ValidationResult.Valid
        }
    }

    fun validateAll(condition: EventCondition): ValidationResult =
        validateObjectId(condition)
            .and(validateFieldKey(condition))
            .and(validateBooleanType(condition))
            .and(validateSingleType(condition))
            .and(validateRangeType(condition))
            .and(validateDisplacementGauge(condition))
}

fun EventCondition.validate(): ValidationResult =
    EventConditionValidators.validateAll(this)

fun EventCondition.validateOrThrow() {
    validate().orThrow()
}

fun <T> combineValidators(vararg validators: Validator<T>): Validator<T> = { value ->
    validators.fold(ValidationResult.Valid as ValidationResult) { acc, validator ->
        acc and validator(value)
    }
}
