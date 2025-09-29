package com.pluxity.aiot.data.dto

import com.influxdb.annotations.Column
import java.time.Instant

data class ClimateSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "DiscomfortIndex") val discomfortIndex: Double? = null,
    @Column(name = "Temperature") val temperature: Double? = null,
    @Column(name = "Humidity") val humidity: Double? = null,
)

data class DisplacementGaugeSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "AngleX") val angleX: Double? = null,
    @Column(name = "AngleY") val angleY: Double? = null,
)

// 메트릭 정의를 위한 데이터 클래스
data class MetricDefinition(
    val key: String,
    val unit: String,
)

// 단일 값을 위한 확장 함수
inline fun <T> buildMetricMap(
    source: T,
    definitions: List<MetricDefinition>,
    valueExtractor: T.(MetricDefinition) -> Double?,
): Map<String, MetricData> =
    buildMap {
        definitions.forEach { definition ->
            source.valueExtractor(definition)?.let { value ->
                put(definition.key, MetricData(definition.unit, value))
            }
        }
    }

// 리스트 값을 위한 확장 함수
inline fun <T> List<T>.buildListMetricMap(
    definitions: List<MetricDefinition>,
    valueExtractor: T.(MetricDefinition) -> Double,
): Map<String, ListMetricData> =
    buildMap {
        definitions.forEach { definition ->
            val values = this@buildListMetricMap.map { it.valueExtractor(definition) }
            put(definition.key, ListMetricData(definition.unit, values))
        }
    }

// 공통 메트릭 정의
object SensorMetrics {
    val TEMPERATURE = MetricDefinition("temperature", "℃")
    val HUMIDITY = MetricDefinition("humidity", "%")
    val DISCOMFORT_INDEX = MetricDefinition("discomfortIndex", "")
    val ANGLE_X = MetricDefinition("angleX", "°")
    val ANGLE_Y = MetricDefinition("angleY", "°")

    val CLIMATE = listOf(TEMPERATURE, HUMIDITY, DISCOMFORT_INDEX)
    val DISPLACEMENT_GAUGE = listOf(ANGLE_X, ANGLE_Y)
}

private fun createDeviceDataResponse(
    deviceId: String,
    time: Instant?,
    metricMap: Map<String, MetricData>,
): DeviceDataResponse {
    val queryInfo = QueryInfo(metricMap.keys.toList())
    val meta = MetaData(deviceId, queryInfo)

    return DeviceDataResponse(
        meta = meta,
        timestamp = time.toString(),
        metrics = metricMap,
    )
}

// 간소화된 확장 함수들
fun ClimateSensorData.toDeviceDataResponse(deviceId: String): DeviceDataResponse = createDeviceDataResponse(deviceId, time, toMetricMap())

fun DisplacementGaugeSensorData.toDeviceDataResponse(deviceId: String): DeviceDataResponse =
    createDeviceDataResponse(deviceId, time, toMetricMap())

private fun ClimateSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.CLIMATE) { definition ->
        when (definition.key) {
            "temperature" -> temperature
            "humidity" -> humidity
            "discomfortIndex" -> discomfortIndex
            else -> null
        }
    }

private fun DisplacementGaugeSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.DISPLACEMENT_GAUGE) { definition ->
        when (definition.key) {
            "angleX" -> angleX
            "angleY" -> angleY
            else -> null
        }
    }
