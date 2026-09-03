package com.pluxity.aiot.data

import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.data.dto.ClimateSensorData
import com.pluxity.aiot.data.dto.CompositeAirQualitySensorData
import com.pluxity.aiot.data.dto.DataResponse
import com.pluxity.aiot.data.dto.ForestFireSensorData
import com.pluxity.aiot.data.dto.ListDataResponse
import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.dto.OdorMonitorSensorData
import com.pluxity.aiot.data.dto.PeopleCounterSensorData
import com.pluxity.aiot.data.dto.SensorMetrics
import com.pluxity.aiot.data.dto.WasteFillLevelSensorData
import com.pluxity.aiot.data.dto.buildListMetricMap
import com.pluxity.aiot.data.dto.climateValueExtractor
import com.pluxity.aiot.data.dto.compositeAirQualityValueExtractor
import com.pluxity.aiot.data.dto.forestFireValueExtractor
import com.pluxity.aiot.data.dto.odorMonitorValueExtractor
import com.pluxity.aiot.data.dto.peopleCounterValueExtractor
import com.pluxity.aiot.data.dto.toDeviceDataResponse
import com.pluxity.aiot.data.dto.wasteFillLevelValueExtractor
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.feature.FeatureService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteService
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
    private val siteService: SiteService,
) {
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
            SensorType.WASTE_FILL_LEVEL -> this.makeWasteFillLevelData(query, deviceId, interval, timeRange)
            SensorType.FOREST_FIRE -> this.makeForestFireData(query, deviceId, interval, timeRange)
            SensorType.ODOR_MONITOR -> this.makeOdorMonitorData(query, deviceId, interval, timeRange)
            SensorType.PEOPLE_COUNTER -> this.makePeopleCounterData(query, deviceId, interval, timeRange)
            SensorType.COMPOSITE_AIR_QUALITY -> this.makeCompositeAirQualityData(query, deviceId, interval, timeRange)
            else -> throw CustomException(ErrorCode.NOT_FOUND_DATA)
        }
    }

    @Transactional(readOnly = true)
    fun getSiteTimeSeries(
        siteId: Long,
        interval: DataInterval,
        from: String,
        to: String,
        sensorType: SensorType,
    ): ListDataResponse {
        siteService.findByIdResponse(siteId)
        val timeRange = Pair(from, to).parseTimeRange()
        val query =
            getTimeSeriesQuery(from, to, Restrictions.tag("siteId").equal(siteId.toString()), sensorType.measureName, interval.unit)
        return when (sensorType) {
            SensorType.TEMPERATURE_HUMIDITY -> this.makeClimateData(query, siteId.toString(), interval, timeRange)
            SensorType.WASTE_FILL_LEVEL -> this.makeWasteFillLevelData(query, siteId.toString(), interval, timeRange)
            SensorType.FOREST_FIRE -> this.makeForestFireData(query, siteId.toString(), interval, timeRange)
            SensorType.ODOR_MONITOR -> this.makeOdorMonitorData(query, siteId.toString(), interval, timeRange)
            SensorType.PEOPLE_COUNTER -> this.makePeopleCounterData(query, siteId.toString(), interval, timeRange)
            SensorType.COMPOSITE_AIR_QUALITY ->
                this.makeCompositeAirQualityData(query, siteId.toString(), interval, timeRange)
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
            SensorType.WASTE_FILL_LEVEL -> {
                getWasteFillLevel(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            SensorType.FOREST_FIRE -> {
                getForestFire(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            SensorType.ODOR_MONITOR -> {
                getOdorMonitor(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            SensorType.PEOPLE_COUNTER -> {
                getPeopleCounter(query).firstOrNull()?.toDeviceDataResponse(deviceId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)
            }
            SensorType.COMPOSITE_AIR_QUALITY -> {
                getCompositeAirQuality(query).firstOrNull()?.toDeviceDataResponse(deviceId)
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
            DateTimeUtils.parseCompactDateTime(first),
            DateTimeUtils.parseCompactDateTime(second),
        )

    private fun makeClimateData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getClimateData(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.CLIMATE_SERIES, climateValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun makeWasteFillLevelData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getWasteFillLevel(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.WASTE_FILL_LEVEL_SERIES, wasteFillLevelValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun makeForestFireData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getForestFire(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.FOREST_FIRE_SERIES, forestFireValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun makeOdorMonitorData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getOdorMonitor(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.ODOR_MONITOR_SERIES, odorMonitorValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun makePeopleCounterData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getPeopleCounter(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.PEOPLE_COUNTER_SERIES, peopleCounterValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun makeCompositeAirQualityData(
        query: String,
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): ListDataResponse {
        val data = getCompositeAirQuality(query)
        val bucketList = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val metrics = data.buildListMetricMap(SensorMetrics.COMPOSITE_AIR_QUALITY_SERIES, compositeAirQualityValueExtractor)
        return createListDataResponse(targetId, interval, timeRange, metrics, bucketList)
    }

    private fun convertUtcToKstString(
        interval: DataInterval,
        time: Instant,
    ): String =
        DateTimeFormatter
            .ofPattern(interval.format)
            .format(time.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())

    private fun getClimateData(query: String) = queryApi.query(query, influxdbProperties.org, ClimateSensorData::class.java)

    private fun getWasteFillLevel(query: String) = queryApi.query(query, influxdbProperties.org, WasteFillLevelSensorData::class.java)

    private fun getForestFire(query: String) = queryApi.query(query, influxdbProperties.org, ForestFireSensorData::class.java)

    private fun getOdorMonitor(query: String) = queryApi.query(query, influxdbProperties.org, OdorMonitorSensorData::class.java)

    private fun getPeopleCounter(query: String) = queryApi.query(query, influxdbProperties.org, PeopleCounterSensorData::class.java)

    private fun getCompositeAirQuality(query: String) =
        queryApi.query(query, influxdbProperties.org, CompositeAirQualitySensorData::class.java)

    private fun createListDataResponse(
        targetId: String,
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
        metrics: Map<String, ListMetricData>,
        bucketList: List<String>,
    ): ListDataResponse =
        ListDataResponse(
            ListMetaData(
                targetId,
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
