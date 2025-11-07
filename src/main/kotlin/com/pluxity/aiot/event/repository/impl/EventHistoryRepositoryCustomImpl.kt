package com.pluxity.aiot.event.repository.impl

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepositoryCustom
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.site.Site
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventHistoryRepositoryCustomImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
    private val entityManager: EntityManager,
    private val renderContext: JpqlRenderContext,
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
        result: EventStatus?,
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
                            result?.let { path(EventHistory::status).eq(it) },
                            path(Site::id).`in`(siteIds),
                        ),
                    ).orderBy(path(EventHistory::id).desc())
            }.filterNotNull()

    override fun findEventListWithPaging(
        from: String?,
        to: String?,
        siteId: Long?,
        result: EventStatus?,
        siteIds: List<Long>,
        size: Int,
        lastId: Long?,
    ): List<EventHistory> {
        val query =
            jpql {
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
                            result?.let { path(EventHistory::status).eq(it) },
                            path(Site::id).`in`(siteIds),
                            lastId?.let { path(EventHistory::id).lt(it) },
                        ),
                    ).orderBy(path(EventHistory::id).desc())
            }

        return entityManager
            .createQuery(query, renderContext)
            .apply { maxResults = size + 1 }
            .resultList
    }
}
