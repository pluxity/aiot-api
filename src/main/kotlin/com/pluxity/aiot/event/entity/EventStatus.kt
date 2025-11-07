package com.pluxity.aiot.event.entity

enum class EventStatus(
    val description: String,
) {
    PENDING("미조치"),
    WORKING("조치중"),
    COMPLETED("조치 완료"),
}
