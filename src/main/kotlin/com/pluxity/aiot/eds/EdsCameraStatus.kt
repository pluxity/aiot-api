package com.pluxity.aiot.eds

enum class EdsCameraStatus(
    val code: Int,
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
