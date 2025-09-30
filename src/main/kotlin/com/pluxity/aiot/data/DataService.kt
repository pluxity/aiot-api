package com.pluxity.aiot.data

import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.dto.ClimateSensorData
import com.pluxity.aiot.data.dto.DataResponse
import com.pluxity.aiot.data.dto.DisplacementGaugeSensorData
import com.pluxity.aiot.data.dto.ListDataResponse
import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.dto.SensorMetrics
import com.pluxity.aiot.data.dto.buildListMetricMap
import com.pluxity.aiot.data.dto.toDeviceDataResponse
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.facility.FacilityService
import com.pluxity.aiot.feature.FeatureService
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
import java.time.temporal.ChronoUnit

@Service
class DataService(
    private val influxdbProperties: InfluxdbProperties,
    private val queryApi: QueryApi,
    private val featureService: FeatureService,
    private val facilityService: FacilityService,
) {
    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }

    @Transactional(readOnly = true)
    fun getFeatureTimeSeries(
        deviceId: String,
        interval: DataInterval,
        from: String,
        to: String,
    ): ListDataResponse {
        val sensorType = getSensorType(deviceId)
        val timeRange = Pair(from, to).parseTimeRange()
        val query = getTimeSeriesQuery(from, to, Restrictions.tag("deviceId").equal(deviceId), sensorType.measureName, interval.unit)
        return when (sensorType) {
            SensorType.TEMPERATURE_HUMIDITY -> this.makeClimateData(query, deviceId, interval, timeRange)
            SensorType.DISPLACEMENT_GAUGE -> this.makeDisplacementGaugeData(query, deviceId, interval, timeRange)
            else -> throw CustomException(ErrorCode.NOT_FOUND_DATA)
        }
    }

    @Transactional(readOnly = true)
    fun getFacilityTimeSeries(
        facilityId: Long,
        interval: DataInterval,
        from: String,
        to: String,
        sensorType: SensorType,
    ): ListDataResponse {
        facilityService.findByIdResponse(facilityId)
        val timeRange = Pair(from, to).parseTimeRange()
        val query =
            getTimeSeriesQuery(from, to, Restrictions.tag("facilityId").equal(facilityId.toString()), sensorType.measureName, interval.unit)
        return when (sensorType) {
            SensorType.TEMPERATURE_HUMIDITY -> this.makeClimateData(query, facilityId.toString(), interval, timeRange)
            SensorType.DISPLACEMENT_GAUGE -> this.makeDisplacementGaugeData(query, facilityId.toString(), interval, timeRange)
            else -> throw CustomException(ErrorCode.NOT_FOUND_DATA)
        }
    }

    @Transactional(readOnly = true)
    fun getFeatureLatestData(deviceId: String): DataResponse {
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

    private fun getTimeSeriesQuery(
        from: String,
        to: String,
        restrictions: Restrictions,
        measureName: String,
        unit: ChronoUnit,
    ): String =
        Flux
            .from(influxdbProperties.bucket)
            .range(DateTimeUtils.toIsoTimeFromKst(from), DateTimeUtils.toIsoTimeFromKst(to))
            .filter(
                Restrictions.and(
                    Restrictions.measurement().equal(measureName),
                    restrictions,
                ),
            ).aggregateWindow(1, unit, "mean")
            .withCreateEmpty(false)
            .filter(
                Restrictions.time().notEqual(DateTimeUtils.toIsoTimeFromKst(to)),
            ).pivot(listOf("_time"), listOf("fieldKey"), "_value")
            .sort(listOf("_time"), false)
            .toString()

    private fun getSensorType(deviceId: String): SensorType {
        val feature = featureService.findByDeviceIdResponse(deviceId)
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
    ): ListDataResponse {
        val data = getClimateData(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.time!!) }
        val metrics =
            data.buildListMetricMap(SensorMetrics.CLIMATE) { definition ->
                when (definition.key) {
                    "temperature" -> temperature!!
                    "humidity" -> humidity!!
                    "discomfortIndex" -> discomfortIndex!!
                    else -> 0.0
                }
            }
        return createListDataResponse(deviceId, interval, timeRange, metrics, bucketList)
    }

    private fun makeDisplacementGaugeData(
        query: String,
        deviceId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getDisplacementGauge(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.time!!) }
        val metrics =
            data.buildListMetricMap(SensorMetrics.DISPLACEMENT_GAUGE) { definition ->
                when (definition.key) {
                    "angleX" -> angleX!!
                    "angleY" -> angleY!!
                    else -> 0.0
                }
            }
        return createListDataResponse(deviceId, interval, timeRange, metrics, bucketList)
    }

    private fun convertUtcToKstString(
        interval: DataInterval,
        time: Instant,
    ): String =
        DateTimeFormatter
            .ofPattern(interval.format)
            .format(time.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())

    private fun getClimateData(query: String) = queryApi.query(query, influxdbProperties.org, ClimateSensorData::class.java)

    private fun getDisplacementGauge(query: String) = queryApi.query(query, influxdbProperties.org, DisplacementGaugeSensorData::class.java)

    private fun createListDataResponse(
        deviceId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
        metrics: Map<String, ListMetricData>,
        bucketList: List<String>,
    ): ListDataResponse =
        ListDataResponse(
            ListMetaData(
                deviceId,
                ListQueryInfo(
                    interval.name,
                    timeRange.first.toString(),
                    timeRange.second.toString(),
                    metrics.keys.toList(),
                ),
            ),
            bucketList,
            metrics,
        )
}
