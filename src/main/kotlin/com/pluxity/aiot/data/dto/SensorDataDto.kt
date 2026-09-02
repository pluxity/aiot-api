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

data class ForestFireSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "FireDetection") val fireDetection: Double? = null,
    @Column(name = "Temperature") val temperature: Double? = null,
    @Column(name = "Humidity") val humidity: Double? = null,
    @Column(name = "CO2") val co2: Double? = null,
    @Column(name = "CO") val co: Double? = null,
    @Column(name = "TVOC") val tvoc: Double? = null,
    @Column(name = "FireCauseMask") val fireCauseMask: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

data class OdorMonitorSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "Temperature") val temperature: Double? = null,
    @Column(name = "Humidity") val humidity: Double? = null,
    @Column(name = "NH3") val nh3: Double? = null,
    @Column(name = "H2S") val h2s: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

data class PeopleCounterSensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "NumberOfVisitors") val numberOfVisitors: Double? = null,
    @Column(name = "NumberOfLeavers") val numberOfLeavers: Double? = null,
) {
    val requiredTime: Instant
        get() = checkNotNull(time) { "_time is missing in InfluxDB query result" }
}

data class CompositeAirQualitySensorData(
    @Column(name = "_time") val time: Instant? = null,
    @Column(name = "Temperature") val temperature: Double? = null,
    @Column(name = "Humidity") val humidity: Double? = null,
    @Column(name = "PM2.5") val pm25: Double? = null,
    @Column(name = "PM10") val pm10: Double? = null,
    @Column(name = "WindSpeed") val windSpeed: Double? = null,
    @Column(name = "WindDirection") val windDirection: Double? = null,
    @Column(name = "UVI") val uvi: Double? = null,
    @Column(name = "LED Light") val ledLight: Double? = null,
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

    /**
     * 조회 대상은 이벤트 조건 대상(deviceProfiles)보다 넓다.
     * ContainerModuleId와 HighThreshold는 적재/조회만 하고 조건 평가에는 쓰지 않는다.
     */
    val WASTE_FILL_LEVEL =
        (
            SensorType.WASTE_FILL_LEVEL.deviceProfiles +
                listOf(DeviceProfileEnum.CONTAINER_MODULE_ID, DeviceProfileEnum.HIGH_THRESHOLD)
        ).map { it.toMetricDefinition() }
    val FOREST_FIRE =
        (
            SensorType.FOREST_FIRE.deviceProfiles +
                listOf(
                    DeviceProfileEnum.TEMPERATURE,
                    DeviceProfileEnum.HUMIDITY,
                    DeviceProfileEnum.CO2,
                    DeviceProfileEnum.CO,
                    DeviceProfileEnum.TVOC,
                    DeviceProfileEnum.FIRE_CAUSE_MASK,
                )
        ).map { it.toMetricDefinition() }
    val ODOR_MONITOR =
        (
            SensorType.ODOR_MONITOR.deviceProfiles +
                listOf(DeviceProfileEnum.TEMPERATURE, DeviceProfileEnum.HUMIDITY)
        ).map { it.toMetricDefinition() }
    val PEOPLE_COUNTER = SensorType.PEOPLE_COUNTER.deviceProfiles.map { it.toMetricDefinition() }
    val COMPOSITE_AIR_QUALITY =
        (
            SensorType.COMPOSITE_AIR_QUALITY.deviceProfiles +
                listOf(DeviceProfileEnum.WIND_DIRECTION, DeviceProfileEnum.LED_LIGHT)
        ).map { it.toMetricDefinition() }
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

fun ForestFireSensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

private fun ForestFireSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.FOREST_FIRE, forestFireValueExtractor)
        .decodeFireCauseMask()

/**
 * FireCauseMask는 InfluxDB에 원값(비트 마스크)으로 적재되므로 응답 시점에만 원인 목록으로 해석한다.
 */
private fun Map<String, MetricData>.decodeFireCauseMask(): Map<String, MetricData> =
    mapValues { (key, metric) ->
        if (key == DeviceProfileEnum.FIRE_CAUSE_MASK.fieldKey) {
            metric.copy(causes = FireCause.decode(metric.value.toInt()).map { it.description })
        } else {
            metric
        }
    }

fun OdorMonitorSensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

private fun OdorMonitorSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.ODOR_MONITOR, odorMonitorValueExtractor)

fun PeopleCounterSensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

private fun PeopleCounterSensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.PEOPLE_COUNTER, peopleCounterValueExtractor)

fun CompositeAirQualitySensorData.toDeviceDataResponse(deviceId: String): DataResponse =
    createDeviceDataResponse(deviceId, requiredTime, toMetricMap())

private fun CompositeAirQualitySensorData.toMetricMap(): Map<String, MetricData> =
    buildMetricMap(this, SensorMetrics.COMPOSITE_AIR_QUALITY, compositeAirQualityValueExtractor)

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

val forestFireValueExtractor: ForestFireSensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.FOREST_FIRE_DETECTION.fieldKey -> fireDetection
        DeviceProfileEnum.TEMPERATURE.fieldKey -> temperature
        DeviceProfileEnum.HUMIDITY.fieldKey -> humidity
        DeviceProfileEnum.CO2.fieldKey -> co2
        DeviceProfileEnum.CO.fieldKey -> co
        DeviceProfileEnum.TVOC.fieldKey -> tvoc
        DeviceProfileEnum.FIRE_CAUSE_MASK.fieldKey -> fireCauseMask
        else -> null
    }
}

val odorMonitorValueExtractor: OdorMonitorSensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.TEMPERATURE.fieldKey -> temperature
        DeviceProfileEnum.HUMIDITY.fieldKey -> humidity
        DeviceProfileEnum.NH3.fieldKey -> nh3
        DeviceProfileEnum.H2S.fieldKey -> h2s
        else -> null
    }
}

val peopleCounterValueExtractor: PeopleCounterSensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.NUMBER_OF_VISITORS.fieldKey -> numberOfVisitors
        DeviceProfileEnum.NUMBER_OF_LEAVERS.fieldKey -> numberOfLeavers
        else -> null
    }
}

val compositeAirQualityValueExtractor: CompositeAirQualitySensorData.(MetricDefinition) -> Double? = { definition ->
    when (definition.key) {
        DeviceProfileEnum.TEMPERATURE.fieldKey -> temperature
        DeviceProfileEnum.HUMIDITY.fieldKey -> humidity
        DeviceProfileEnum.PM2_5.fieldKey -> pm25
        DeviceProfileEnum.PM10.fieldKey -> pm10
        DeviceProfileEnum.WIND_SPEED.fieldKey -> windSpeed
        DeviceProfileEnum.WIND_DIRECTION.fieldKey -> windDirection
        DeviceProfileEnum.UVI.fieldKey -> uvi
        DeviceProfileEnum.LED_LIGHT.fieldKey -> ledLight
        else -> null
    }
}
