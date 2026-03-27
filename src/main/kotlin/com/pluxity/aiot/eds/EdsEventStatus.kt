package com.pluxity.aiot.eds

enum class EdsEventStatus(val code: Int, val description: String) {
    STARTED(1, "이벤트시작"),
    IN_PROGRESS(2, "이벤트진행중"),
    ENDED(4, "이벤트종료"),
    ;

    companion object {
        fun fromCode(code: Int): EdsEventStatus? = entries.find { it.code == code }
    }
}
