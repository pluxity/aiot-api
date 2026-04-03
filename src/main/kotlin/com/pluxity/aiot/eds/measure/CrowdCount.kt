package com.pluxity.aiot.eds.measure

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import java.time.Instant

@Measurement(name = "crowd_count")
class CrowdCount(
    @Column(tag = true) val cameraId: String,
    @Column(tag = true) val eventZoneId: String,
    @Column(tag = true) val eventZoneName: String,
    @Column val total: Long,
    @Column val eventZoneCnt: Long,
    @Column val eventZoneLevel: Long,
    @Column val eventZoneAvg: Double,
    @Column(timestamp = true) val time: Instant,
)
