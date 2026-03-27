package com.pluxity.aiot.eds

import com.fasterxml.jackson.annotation.JsonValue

enum class EdsCameraStatus(
    @JsonValue val code: Int,
    val description: String,
) {
    DISCONNECTED(0, "미연동"),
    NORMAL(1, "정상"),
    FAULT(2, "장애"),
    ;

    companion object {
        fun fromCode(code: Int): EdsCameraStatus = entries.find { it.code == code } ?: DISCONNECTED
    }
}
