package com.pluxity.aiot.event.repository.impl

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.EventHistoryRow
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepositoryCustom
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class EventHistoryRepositoryCustomImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
    private val entityManager: EntityManager,
    private val renderContext: JpqlRenderContext,
) : EventHistoryRepositoryCustom {
    override fun findEventList(
        from: String?,
        to: String?,
        siteId: Long?,
        result: EventStatus?,
        siteIds: List<Long>,
    ): List<EventHistoryRow> =
        kotlinJdslJpqlExecutor
            .findAll {
                selectFromEventHistoryRow()
                    .where(
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
        level: ConditionLevel?,
        sensorType: SensorType?,
        siteIds: List<Long>,
        size: Int,
        lastId: Long?,
    ): List<EventHistoryRow> {
        val fieldKeys = sensorType?.deviceProfiles?.map { it.fieldKey }
        val query =
            jpql {
                selectFromEventHistoryRow()
                    .where(
                        and(
                            from?.let { path(EventHistory::occurredAt).greaterThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) },
                            to?.let { path(EventHistory::occurredAt).lessThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) },
                            siteId?.let { path(Site::id).eq(it) },
                            result?.let { path(EventHistory::status).eq(it) },
                            level?.let { path(EventHistory::level).eq(it) },
                            path(Site::id).`in`(siteIds),
                            lastId?.let { path(EventHistory::id).lt(it) },
                            fieldKeys?.takeIf { it.isNotEmpty() }?.let { path(EventHistory::fieldKey).`in`(fieldKeys) },
                        ),
                    ).orderBy(path(EventHistory::id).desc())
            }

        return entityManager
            .createQuery(query, renderContext)
            .apply { maxResults = size + 1 }
            .resultList
    }

    private fun Jpql.selectFromEventHistoryRow() =
        selectNew<EventHistoryRow>(
            path(EventHistory::id),
            path(EventHistory::deviceId),
            path(EventHistory::objectId),
            path(EventHistory::occurredAt),
            path(EventHistory::minValue),
            path(EventHistory::maxValue),
            path(EventHistory::status),
            path(EventHistory::eventName),
            path(EventHistory::fieldKey),
            path(EventHistory::guideMessage),
            path(Feature::longitude),
            path(Feature::latitude),
            path(EventHistory::updatedBy),
            path(EventHistory::updatedAt),
            path(EventHistory::value),
            path(EventHistory::level),
            path(Site::id),
            path(Site::name),
            path(EventHistory::sensorDescription),
        ).from(
            entity(EventHistory::class),
            join(entity(Feature::class)).on(path(EventHistory::deviceId).equal(path(Feature::deviceId))),
            join(Feature::site),
        )
}
