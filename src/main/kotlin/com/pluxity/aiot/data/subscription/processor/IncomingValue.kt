package com.pluxity.aiot.data.subscription.processor

sealed interface IncomingValue {
    data class Numeric(
        val value: Double,
    ) : IncomingValue

    data class Bool(
        val value: Boolean,
    ) : IncomingValue
}

fun IncomingValue.toEventHistoryValue(): Double =
    when (this) {
        is IncomingValue.Numeric -> value
        is IncomingValue.Bool -> if (value) 1.0 else 0.0
    }
