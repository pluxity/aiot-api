package com.pluxity.aiot.event

import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.dto.buildListMetricMap
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.EventMetrics
import com.pluxity.aiot.event.dto.EventResponse
import com.pluxity.aiot.event.dto.EventTimeSeriesDataResponse
import com.pluxity.aiot.event.dto.toEventResponse
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.response.CursorPageResponse
import com.pluxity.aiot.global.response.toCursorPageResponse
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class EventService(
    private val eventHistoryRepository: EventHistoryRepository,
    private val siteRepository: SiteRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun findAll(
        from: String?,
        to: String?,
        siteId: Long?,
        status: EventStatus?,
        level: ConditionLevel?,
        sensorType: SensorType?,
        size: Int,
        lastId: Long? = null,
    ): CursorPageResponse<EventResponse> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        val eventList =
            eventHistoryRepository
                .findEventListWithPaging(from, to, siteId, status, level, sensorType, siteIds, size, lastId)
                .map { it.toEventResponse() }
        val hasNext = eventList.size > size
        return eventList.toCursorPageResponse(hasNext) { it.eventId }
    }

    @Transactional
    fun updateStatus(
        id: Long,
        result: EventStatus,
    ) {
        val eventHistory = findById(id)
        eventHistory.changeStatus(result)
    }

    fun findById(id: Long): EventHistory =
        eventHistoryRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_HISTORY, id)

    fun getPeriodData(
        interval: DataInterval,
        from: String,
        to: String,
    ): EventTimeSeriesDataResponse {
        val timeRange = Pair(from, to).parseTimeRange()
        val (start, end) = timeRange

        val params =
            mapOf(
                "start" to start,
                "end" to end,
            )

        val result =
            jdbcTemplate.query(
                buildPeriodDataQuery(interval, from, to),
                params,
            ) { rs, _ ->
                val bucket = rs.getObject("bucket_start", LocalDateTime::class.java)
                val p = rs.getInt("pending_cnt")
                val w = rs.getInt("working_cnt")
                val c = rs.getInt("completed_cnt")
                EventListDto(
                    bucket.format(DateTimeFormatter.ofPattern(interval.format)),
                    p,
                    w,
                    c,
                )
            }
        return result.toDeviceListDataResponse(interval, timeRange)
    }

    data class EventListDto(
        val bucketStart: String, // date_trunc로 만든 버킷 시작 시각
        val pendingCnt: Int,
        val workingCnt: Int,
        val completedCnt: Int,
    )

    private fun List<EventListDto>.toDeviceListDataResponse(
        interval: DataInterval,
        timeRange: Pair<LocalDateTime, LocalDateTime>,
    ): EventTimeSeriesDataResponse {
        val bucketList = map { it.bucketStart }
        val metrics = toMetricsMap()
        val metaData =
            ListMetaData(
                "",
                ListQueryInfo(
                    interval.name,
                    timeRange.first.toString(),
                    timeRange.second.toString(),
                    metrics.keys.toList(),
                ),
            )

        return EventTimeSeriesDataResponse(metaData, bucketList, metrics)
    }

    private fun List<EventListDto>.toMetricsMap(): Map<String, ListMetricData> =
        buildListMetricMap(EventMetrics.ALL) { definition ->
            when (definition.key) {
                "pendingCnt" -> pendingCnt.toDouble()
                "workingCnt" -> workingCnt.toDouble()
                "completedCnt" -> completedCnt.toDouble()
                else -> 0.0
            }
        }

    private fun buildPeriodDataQuery(
        interval: DataInterval,
        from: String,
        to: String,
    ): String =
        """
        WITH buckets AS (
            SELECT generate_series(
                date_trunc('${interval.pgUnit}', to_timestamp('$from', 'YYYYMMDDHH24MISS')),
                date_trunc('${interval.pgUnit}', to_timestamp('$to', 'YYYYMMDDHH24MISS')),
                (interval '1 ${interval.pgUnit}')::interval
            ) AS bucket_start
        )
        SELECT
            b.bucket_start::timestamp AS bucket_start,
            COUNT(*) FILTER (WHERE e.status = 'PENDING')   AS pending_cnt,
            COUNT(*) FILTER (WHERE e.status = 'WORKING')   AS working_cnt,
            COUNT(*) FILTER (WHERE e.status = 'COMPLETED') AS completed_cnt
        FROM buckets b
        LEFT JOIN event_history e
          ON e.occurred_at >= b.bucket_start
         AND e.occurred_at <  b.bucket_start + (interval '1 ${interval.pgUnit}')::interval
        GROUP BY b.bucket_start
        ORDER BY b.bucket_start;
        """.trimIndent()

    private fun Pair<String, String>.parseTimeRange(): Pair<LocalDateTime, LocalDateTime> =
        Pair(
            DateTimeUtils.parseCompactDateTime(first),
            DateTimeUtils.parseCompactDateTime(second),
        )
}
