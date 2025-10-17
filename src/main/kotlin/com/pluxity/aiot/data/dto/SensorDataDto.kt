package com.pluxity.aiot.data.dto

import com.influxdb.annotations.Column
import com.pluxity.aiot.alarm.type.DeviceProfileEnum
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
    val TEMPERATURE = MetricDefinition(DeviceProfileEnum.TEMPERATURE.fieldKey, DeviceProfileEnum.TEMPERATURE.unit)
    val HUMIDITY = MetricDefinition(DeviceProfileEnum.HUMIDITY.fieldKey, DeviceProfileEnum.HUMIDITY.unit)
    val DISCOMFORT_INDEX = MetricDefinition(DeviceProfileEnum.DISCOMFORT_INDEX.fieldKey, DeviceProfileEnum.DISCOMFORT_INDEX.unit)
    val ANGLE_X = MetricDefinition(DeviceProfileEnum.ANGLE_X.fieldKey, DeviceProfileEnum.ANGLE_X.unit)
    val ANGLE_Y = MetricDefinition(DeviceProfileEnum.ANGLE_Y.fieldKey, DeviceProfileEnum.ANGLE_Y.unit)

    val CLIMATE = listOf(TEMPERATURE, HUMIDITY, DISCOMFORT_INDEX)
    val DISPLACEMENT_GAUGE = listOf(ANGLE_X, ANGLE_Y)
}

private fun createDeviceDataResponse(
    deviceId: String,
    time: Instant,
    metricMap: Map<String, MetricData>,
): DataResponse {
    val queryInfo = QueryInfo(metricMap.keys.toList())
    val meta = MetaData(deviceId, queryInfo)

    return DataResponse(
        meta = meta,
        timestamp = time.toString(),
        metrics = metricMap,
    )
}

// 간소화된 확장 함수들
fun ClimateSensorData.toDeviceDataResponse(deviceId: String): DataResponse = createDeviceDataResponse(deviceId, time!!, toMetricMap())

fun DisplacementGaugeSensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, time!!, toMetricMap())

private fun ClimateSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.CLIMATE) { definition ->
        when (definition.key) {
            DeviceProfileEnum.TEMPERATURE.fieldKey -> temperature
            DeviceProfileEnum.HUMIDITY.fieldKey -> humidity
            DeviceProfileEnum.DISCOMFORT_INDEX.fieldKey -> discomfortIndex
            else -> null
        }
    }

private fun DisplacementGaugeSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.DISPLACEMENT_GAUGE) { definition ->
        when (definition.key) {
            DeviceProfileEnum.ANGLE_X.fieldKey -> angleX
            DeviceProfileEnum.ANGLE_Y.fieldKey -> angleY
            else -> null
        }
    }
