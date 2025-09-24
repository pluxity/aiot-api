package com.pluxity.aiot.sensor.dto

import com.influxdb.annotations.Column

data class LastSensorData(
    @Column(name = "_time") val time: java.time.Instant? = null,
    @Column(name = "deviceId") val device: String = "",
)
