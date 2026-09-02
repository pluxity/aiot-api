package com.pluxity.aiot.mic

enum class MicStatus {
    ACTIVE,
    INACTIVE,

    /** 벤더 장비 목록에서 사라진 상태 */
    DISCONNECTED,
    ;

    companion object {
        fun fromValue(value: String?): MicStatus? = entries.find { it.name.equals(value, ignoreCase = true) }
    }
}
