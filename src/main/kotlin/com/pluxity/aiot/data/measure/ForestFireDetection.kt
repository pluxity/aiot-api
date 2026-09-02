package com.pluxity.aiot.data.measure

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import java.time.Instant

@Measurement(name = "forest_fire_detection")
class ForestFireDetection(
    @Column(tag = true) val siteId: String,
    @Column(tag = true) val deviceId: String,
    @Column(tag = true) val fieldKey: String,
    @Column val value: Double,
    @Column(timestamp = true) val time: Instant,
)
