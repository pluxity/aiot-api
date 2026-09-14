package com.pluxity.aiot.incident

enum class IncidentSourceType(
    val description: String,
) {
    SENSOR("센서"),
    CCTV("CCTV"),
    MIC("AI 마이크"),
}
