package com.pluxity.aiot.eds.dto

import com.influxdb.annotations.Column
import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.MetricDefinition
import java.time.Instant

data class CrowdCountSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "_value") val total: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

data class CrowdCountTimeSeriesResponse(
    val meta: ListMetaData,
    val timestamps: List<String>,
    val metrics: Map<String, ListMetricData>,
)

data class CrowdCountLatestResponse(
    val cameraId: String,
    val total: Int,
    val timestamp: String,
)

object CrowdCountMetrics {
    val TOTAL = MetricDefinition("total", "명")
    val ALL = listOf(TOTAL)
}
