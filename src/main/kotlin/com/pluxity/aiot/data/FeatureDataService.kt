package com.pluxity.aiot.data

import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.dto.ClimateSensorData
import com.pluxity.aiot.data.dto.DeviceDataResponse
import com.pluxity.aiot.data.dto.DeviceListDataResponse
import com.pluxity.aiot.data.dto.DisplacementGaugeSensorData
import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.dto.SensorMetrics
import com.pluxity.aiot.data.dto.buildListMetricMap
import com.pluxity.aiot.data.dto.toDeviceDataResponse
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.utils.DateTimeUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class FeatureDataService(
    private val influxdbProperties: InfluxdbProperties,
    private val queryApi: QueryApi,
    private val featureRepository: FeatureRepository,
) {
    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }

    @Transactional(readOnly = true)
    fun getTimeSeries(
        deviceId: String,
        interval: DataInterval,
        from: String,
        to: String,
    ): DeviceListDataResponse {
        val sensorType = getSensorType(deviceId)
        val timeRange = Pair(from, to).parseTimeRange()
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(DateTimeUtils.toIsoTimeFromKst(from), DateTimeUtils.toIsoTimeFromKst(to))
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(sensorType.measureName),
                        Restrictions.tag("deviceId").equal(deviceId),
                    ),
                ).aggregateWindow(1, interval.unit, "mean")
                .withCreateEmpty(false)
                .pivot(listOf("_time"), listOf("fieldKey"), "_value")
                .sort(listOf("_time"), false)
                .toString()
        return when (sensorType) {
            SensorType.TEMPERATURE_HUMIDITY -> this.makeClimateData(query, deviceId, interval, timeRange)
            SensorType.DISPLACEMENT_GAUGE -> this.makeDisplacementGaugeData(query, deviceId, interval, timeRange)
            else -> throw CustomException(ErrorCode.NOT_FOUND_DATA)
        }
    }

    @Transactional(readOnly = true)
    fun getLatestData(deviceId: String): DeviceDataResponse {
        val sensorType = getSensorType(deviceId)
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(0)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(sensorType.measureName),
                        Restrictions.tag("deviceId").equal(deviceId),
                    ),
                ).pivot(listOf("_time"), listOf("fieldKey"), "_value")
                .sort(listOf("_time"), true)
                .limit(1)
                .toString()
        return when (sensorType) {
            SensorType.TEMPERATURE_HUMIDITY -> {
                getClimateData(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            SensorType.DISPLACEMENT_GAUGE -> {
                getDisplacementGauge(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            else -> throw CustomException(ErrorCode.NOT_FOUND_DATA)
        }
    }

    private fun getSensorType(deviceId: String): SensorType {
        val feature =
            featureRepository.findByDeviceId(deviceId) ?: throw CustomException(
                ErrorCode.NOT_FOUND_DEVICE_BY_FEATURE,
                deviceId,
            )
        return SensorType.fromObjectId(feature.objectId.take(5))
    }

    private fun Pair<String, String>.parseTimeRange(): Pair<LocalDateTime, LocalDateTime> =
        Pair(
            LocalDateTime.parse(first, FORMATTER),
            LocalDateTime.parse(second, FORMATTER),
        )

    private fun makeClimateData(
        query: String,
        deviceId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): DeviceListDataResponse {
        val data = getClimateData(query)
        return createDeviceListDataResponse(
            data = data,
            deviceId = deviceId,
            interval = interval,
            timeRange = timeRange,
            timeExtractor = { it.time!! },
            metricsBuilder = {
                it.buildListMetricMap(SensorMetrics.CLIMATE) { definition ->
                    when (definition.key) {
                        "temperature" -> temperature!!
                        "humidity" -> humidity!!
                        "discomfortIndex" -> discomfortIndex!!
                        else -> 0.0
                    }
                }
            },
        )
    }

    private fun getClimateData(query: String) = queryApi.query(query, influxdbProperties.org, ClimateSensorData::class.java)

    private fun getDisplacementGauge(query: String) = queryApi.query(query, influxdbProperties.org, DisplacementGaugeSensorData::class.java)

    private fun makeDisplacementGaugeData(
        query: String,
        deviceId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): DeviceListDataResponse {
        val data = getDisplacementGauge(query)
        return createDeviceListDataResponse(
            data = data,
            deviceId = deviceId,
            interval = interval,
            timeRange = timeRange,
            timeExtractor = { it.time!! },
            metricsBuilder = {
                it.buildListMetricMap(SensorMetrics.DISPLACEMENT_GAUTE) { definition ->
                    when (definition.key) {
                        "angleX" -> angleX!!
                        "angleY" -> angleY!!
                        else -> 0.0
                    }
                }
            },
        )
    }

    private fun <T> createDeviceListDataResponse(
        data: List<T>,
        deviceId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
        timeExtractor: (T) -> Instant,
        metricsBuilder: (List<T>) -> Map<String, ListMetricData>,
    ): DeviceListDataResponse {
        val bucketList =
            data.map {
                DateTimeFormatter
                    .ofPattern(interval.format)
                    .format(timeExtractor(it).atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
            }
        val metrics = metricsBuilder(data)
        val metaData =
            ListMetaData(
                deviceId,
                ListQueryInfo(
                    interval.name,
                    timeRange.first.toString(),
                    timeRange.second.toString(),
                    metrics.keys.toList(),
                ),
            )
        return DeviceListDataResponse(metaData, bucketList, metrics)
    }
}
