package com.pluxity.aiot.eds

enum class EdsCameraType(
    val code: String,
    val description: String,
) {
    IP("ip", "IP 카메라"),
    VIDEO("video", "VIDEO 카메라"),
    PTZ("ptz", "PTZ 카메라"),
    AIBOX("aibox", "AI BOX 카메라"),
    AI("ai", "AI 카메라"),
    UNMANAGED("unmanaged", "미관리 카메라"),
    UNSUPPORTED("unsupported", "미지원 카메라"),
    ;

    companion object {
        fun fromCode(code: String): EdsCameraType = entries.find { it.code == code } ?: UNSUPPORTED
    }
}
