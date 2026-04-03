package com.pluxity.aiot.eds

import com.influxdb.client.QueryApi
import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.dto.ListDataResponse
import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.eds.dto.CrowdCountLatestResponse
import com.pluxity.aiot.eds.dto.CrowdCountMetrics
import com.pluxity.aiot.eds.dto.CrowdCountSensorData
import com.pluxity.aiot.eds.dto.EdsCrowdCountData
import com.pluxity.aiot.eds.measure.CrowdCount
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.utils.DateTimeUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

private val FRAME_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
private const val MEASUREMENT_NAME = "crowd_count"

@Service
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsCrowdCountService(
    private val writeApi: WriteApi,
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
) {
    fun save(data: EdsCrowdCountData) {
        val timestamp = parseFrameTime(data.frameTime)

        if (data.zones.isNullOrEmpty()) {
            val measurement =
                CrowdCount(
                    cameraId = data.cameraId,
                    eventZoneId = "0",
                    eventZoneName = "",
                    total = data.total.toLong(),
                    eventZoneCnt = 0,
                    eventZoneLevel = 0,
                    eventZoneAvg = 0.0,
                    time = timestamp,
                )
            writeApi.writeMeasurement(WritePrecision.S, measurement)
        } else {
            data.zones.forEach { zone ->
                val measurement =
                    CrowdCount(
                        cameraId = data.cameraId,
                        eventZoneId = (zone.eventZoneId ?: 0).toString(),
                        eventZoneName = zone.eventZoneName ?: "",
                        total = data.total.toLong(),
                        eventZoneCnt = (zone.eventZoneCnt ?: 0).toLong(),
                        eventZoneLevel = (zone.eventZoneLevel ?: 0).toLong(),
                        eventZoneAvg = zone.eventZoneAvg ?: 0.0,
                        time = timestamp,
                    )
                writeApi.writeMeasurement(WritePrecision.S, measurement)
            }
        }
    }

    fun getTimeSeries(
        edsCameraId: String,
        interval: DataInterval,
        from: String,
        to: String,
    ): ListDataResponse {
        val fromInstant = DateTimeUtils.toIsoTimeFromKst(from)
        val toInstant = DateTimeUtils.toIsoTimeFromKst(to)
        val query =
            """
            from(bucket: "${influxdbProperties.bucket}")
                |> range(start: $fromInstant, stop: $toInstant)
                |> filter(fn: (r) => r._measurement == "$MEASUREMENT_NAME" and r.cameraId == "$edsCameraId" and r._field == "total")
                |> group(columns: ["_field"])
                |> aggregateWindow(every: 1${interval.fluxUnit}, fn: mean, createEmpty: false)
                |> sort(columns: ["_time"])
            """.trimIndent()

        val data = queryApi.query(query, influxdbProperties.org, CrowdCountSensorData::class.java)

        val timestamps = data.map { convertUtcToKstString(interval, it.requiredTime) }
        val timeRange = Pair(DateTimeUtils.parseCompactDateTime(from), DateTimeUtils.parseCompactDateTime(to))
        val metricKeys = CrowdCountMetrics.ALL.map { it.key }
        val metrics =
            CrowdCountMetrics.ALL.associate { definition ->
                definition.key to ListMetricData(definition.unit, data.map { it.total })
            }

        return ListDataResponse(
            meta =
                ListMetaData(
                    targetId = edsCameraId,
                    query =
                        ListQueryInfo(
                            timeUnit = interval.name,
                            from = timeRange.first.toString(),
                            to = timeRange.second.toString(),
                            metrics = metricKeys,
                        ),
                ),
            timestamps = timestamps,
            metrics = metrics,
        )
    }

    fun getLatest(edsCameraId: String): CrowdCountLatestResponse {
        val query =
            """
            from(bucket: "${influxdbProperties.bucket}")
                |> range(start: -7d)
                |> filter(fn: (r) => r._measurement == "$MEASUREMENT_NAME" and r.cameraId == "$edsCameraId" and r._field == "total")
                |> group(columns: ["_field"])
                |> last()
            """.trimIndent()

        val data = queryApi.query(query, influxdbProperties.org, CrowdCountSensorData::class.java)
        val latest = data.firstOrNull() ?: throw CustomException(ErrorCode.NOT_FOUND_DATA)

        return CrowdCountLatestResponse(
            cameraId = edsCameraId,
            total = latest.total?.toInt() ?: 0,
            timestamp =
                latest.requiredTime
                    .atZone(ZoneId.of("Asia/Seoul"))
                    .toLocalDateTime()
                    .toString(),
        )
    }

    private fun parseFrameTime(frameTime: String): Instant =
        try {
            val ldt = LocalDateTime.parse(frameTime, FRAME_TIME_FORMATTER)
            ZonedDateTime.of(ldt, ZoneId.of("Asia/Seoul")).toInstant()
        } catch (e: Exception) {
            log.warn(e) { "frame_time 파싱 실패: $frameTime, Instant.now() 사용" }
            Instant.now()
        }

    private fun convertUtcToKstString(
        interval: DataInterval,
        time: Instant,
    ): String =
        DateTimeFormatter
            .ofPattern(interval.format)
            .format(time.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime())
}
