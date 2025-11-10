package com.pluxity.aiot.dashboard

data class SensorSummary(
    val siteId: Long,
    val siteName: String,
    val totalSensors: Long,
    val connectionStatus: ConnectionStatus,
    val sensorTypeStatus: SensorTypeStatus,
)

data class ConnectionStatus(
    val connected: Long,
    val disconnected: Long,
)

data class SensorTypeStatus(
    val temperatureHumidity: Long,
    val fire: Long,
    val displacement: Long,
)
