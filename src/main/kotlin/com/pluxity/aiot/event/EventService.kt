package com.pluxity.aiot.event

import com.pluxity.aiot.data.dto.ListMetaData
import com.pluxity.aiot.data.dto.ListMetricData
import com.pluxity.aiot.data.dto.ListQueryInfo
import com.pluxity.aiot.data.dto.buildListMetricMap
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.event.dto.EventMetrics
import com.pluxity.aiot.event.dto.EventResponse
import com.pluxity.aiot.event.dto.EventTimeSeriesDataResponse
import com.pluxity.aiot.event.dto.toEventResponse
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.HistoryResult
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.utils.DateTimeUtils
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
        status: HistoryResult?,
    ): List<EventResponse> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        return eventHistoryRepository.findEventList(from, to, siteId, status, siteIds).map { it.toEventResponse() }
    }

    @Transactional
    fun updateStatus(
        id: Long,
        result: HistoryResult,
    ) {
        val eventHistory = findById(id)
        eventHistory.changeActionResult(result)
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
                buildPeriodDataQuery(interval),
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

    private fun buildPeriodDataQuery(interval: DataInterval): String =
        """
        SELECT
            date_trunc('${interval.unit}', e.occurred_at) AS bucket_start,
            COALESCE(COUNT(CASE WHEN action_result = 'PENDING' THEN 1 END), 0) AS pending_cnt,
            COALESCE(COUNT(CASE WHEN action_result = 'WORKING' THEN 1 END), 0) AS working_cnt,
            COALESCE(COUNT(CASE WHEN action_result = 'COMPLETED' THEN 1 END), 0) AS completed_cnt
        FROM event_history e
        WHERE e.occurred_at BETWEEN :start AND :end
        GROUP BY date_trunc('${interval.unit}', e.occurred_at)
        ORDER BY date_trunc('${interval.unit}', e.occurred_at) ASC
        """.trimIndent()

    private fun Pair<String, String>.parseTimeRange(): Pair<LocalDateTime, LocalDateTime> =
        Pair(
            DateTimeUtils.parseCompactDateTime(first),
            DateTimeUtils.parseCompactDateTime(second),
        )
}
