package com.pluxity.aiot.event.entity

enum class EventStatus(
    val description: String,
) {
    ACTIVE("미조치"),
    IN_PROGRESS("조치중"),
    RESOLVED("조치 완료"),
}
