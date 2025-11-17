package com.pluxity.aiot.event.entity

enum class EventStatus(
    val description: String,
    val querySelector: String,
    val metricKey: String,
) {
    ACTIVE("미조치", "active_cnt", "activeCnt"),
    IN_PROGRESS("조치중", "in_progress_cnt", "inProgressCnt"),
    RESOLVED("조치 완료", "resolved_cnt", "resolvedCnt"),
}
