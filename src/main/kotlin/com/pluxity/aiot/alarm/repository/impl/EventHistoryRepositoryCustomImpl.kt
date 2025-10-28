package com.pluxity.aiot.alarm.repository.impl

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.entity.HistoryResult
import com.pluxity.aiot.alarm.repository.EventHistoryRepositoryCustom
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.site.Site
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventHistoryRepositoryCustomImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : EventHistoryRepositoryCustom {
    override fun findByOccurredAtBetween(
        sensorDescription: String?,
        keyword: String?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
    ): List<EventHistory> =
        kotlinJdslJpqlExecutor
            .findAll {
                select(entity(EventHistory::class))
                    .from(
                        entity(EventHistory::class),
                        join(entity(Feature::class)).on(
                            path(EventHistory::deviceId).equal(path(Feature::deviceId)),
                        ),
                    ).where(
                        and(
                            sensorDescription?.let { path(EventHistory::sensorDescription).equal(it) },
                            keyword?.let { path(Feature::deviceId).like("%$it%") },
                            if (startTime != null && endTime != null) {
                                path(EventHistory::occurredAt).between(startTime, endTime)
                            } else {
                                null
                            },
                        ),
                    )
            }.filterNotNull()

    override fun findEventList(
        from: String?,
        to: String?,
        siteId: Long?,
        result: HistoryResult?,
        siteIds: List<Long>,
    ): List<EventHistory> =
        kotlinJdslJpqlExecutor
            .findAll {
                select(entity(EventHistory::class))
                    .from(
                        entity(EventHistory::class),
                        join(entity(Feature::class)).on(path(EventHistory::deviceId).equal(path(Feature::deviceId))),
                        join(Feature::site),
                    ).where(
                        and(
                            from?.let { path(EventHistory::occurredAt).greaterThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) },
                            to?.let { path(EventHistory::occurredAt).lessThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) },
                            siteId?.let { path(Site::id).eq(it) },
                            result?.let { path(EventHistory::actionResult).eq(it) },
                            path(Site::id).`in`(siteIds),
                        ),
                    )
            }.filterNotNull()
}
