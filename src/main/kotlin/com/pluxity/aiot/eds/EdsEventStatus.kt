package com.pluxity.aiot.eds

import com.fasterxml.jackson.annotation.JsonCreator

enum class EdsEventStatus(
    val code: Int,
    val description: String,
) {
    STARTED(1, "이벤트시작"),
    IN_PROGRESS(2, "이벤트진행중"),
    ENDED(4, "이벤트종료"),
    ;

    companion object {
        @JsonCreator
        fun fromCode(code: Int): EdsEventStatus? = entries.find { it.code == code }
    }
}
