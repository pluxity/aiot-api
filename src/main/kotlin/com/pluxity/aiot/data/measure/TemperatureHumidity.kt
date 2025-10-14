package com.pluxity.aiot.data.measure

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import java.time.Instant

@Measurement(name = "temperature_humidity")
class TemperatureHumidity(
    @Column(tag = true) val siteId: String,
    @Column(tag = true) val deviceId: String,
    @Column val value: Double,
    @Column(tag = true) val fieldKey: String,
    @Column(timestamp = true) val time: Instant,
)
