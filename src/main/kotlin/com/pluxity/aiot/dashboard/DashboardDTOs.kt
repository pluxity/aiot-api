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
    val wasteFillLevel: Long,
    val forestFire: Long,
    val odorMonitor: Long,
    val peopleCounter: Long,
    val compositeAirQuality: Long,
)

data class SensorStatisticsRaw(
    val siteId: Long,
    val siteName: String,
    val totalCount: Long,
    val disconnectedCount: Long,
    val connectedCount: Long,
    val temperatureHumidityCount: Long,
    val fireCount: Long,
    val wasteFillLevelCount: Long,
    val forestFireCount: Long,
    val odorMonitorCount: Long,
    val peopleCounterCount: Long,
    val compositeAirQualityCount: Long,
)

fun SensorStatisticsRaw.toSensorSummary() =
    SensorSummary(
        siteId = siteId,
        siteName = siteName,
        totalSensors = totalCount,
        connectionStatus =
            ConnectionStatus(
                connected = connectedCount,
                disconnected = disconnectedCount,
            ),
        sensorTypeStatus =
            SensorTypeStatus(
                temperatureHumidity = temperatureHumidityCount,
                fire = fireCount,
                wasteFillLevel = wasteFillLevelCount,
                forestFire = forestFireCount,
                odorMonitor = odorMonitorCount,
                peopleCounter = peopleCounterCount,
                compositeAirQuality = compositeAirQualityCount,
            ),
    )
