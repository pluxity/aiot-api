package com.pluxity.aiot.data.dto

import com.influxdb.annotations.Column
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import java.time.Instant
import java.time.ZoneId

data class ClimateSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "DiscomfortIndex") val discomfortIndex: Double? = null,
    @Column(name = "Temperature") val temperature: Double? = null,
    @Column(name = "Humidity") val humidity: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

data class WasteFillLevelSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "ContainerModuleId") val containerModuleId: Double? = null,
    @Column(name = "ActualFilling") val actualFilling: Double? = null,
    @Column(name = "HighThreshold") val highThreshold: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

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
    valueExtractor: T.(MetricDefinition) -> Double?,
): Map<String, ListMetricData> =
    buildMap {
        definitions.forEach { definition ->
            val values = this@buildListMetricMap.map { it.valueExtractor(definition) }
            put(definition.key, ListMetricData(definition.unit, values))
        }
    }

// 공통 메트릭 정의
object SensorMetrics {
    val CLIMATE = SensorType.TEMPERATURE_HUMIDITY.deviceProfiles.map { it.toMetricDefinition() }
    val WASTE_FILL_LEVEL = SensorType.WASTE_FILL_LEVEL.deviceProfiles.map { it.toMetricDefinition() }
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
        timestamp = time.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime().toString(),
        metrics = metricMap,
    )
}

fun ClimateSensorData.toDeviceDataResponse(deviceId: String): DataResponse = createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

fun WasteFillLevelSensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

private fun ClimateSensorData.toMetricMap(): Map<String, MetricData> = buildMetricMap(this, SensorMetrics.CLIMATE, climateValueExtractor)

private fun WasteFillLevelSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.WASTE_FILL_LEVEL, wasteFillLevelValueExtractor)

val climateValueExtractor: ClimateSensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.TEMPERATURE.fieldKey -> temperature
        DeviceProfileEnum.HUMIDITY.fieldKey -> humidity
        DeviceProfileEnum.DISCOMFORT_INDEX.fieldKey -> discomfortIndex
        else -> null
    }
}

val wasteFillLevelValueExtractor: WasteFillLevelSensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.CONTAINER_MODULE_ID.fieldKey -> containerModuleId
        DeviceProfileEnum.ACTUAL_FILLING.fieldKey -> actualFilling
        DeviceProfileEnum.HIGH_THRESHOLD.fieldKey -> highThreshold
        else -> null
    }
}
