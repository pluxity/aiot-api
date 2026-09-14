package com.pluxity.aiot.incident

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.predicate.Predicate
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.dto.IncidentRow
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.global.utils.findAllNotNull
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

@Repository
class IncidentCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
    private val entityManager: EntityManager,
    private val renderContext: JpqlRenderContext,
) : IncidentCustomRepository {
    override fun findEventList(
        from: String?,
        to: String?,
        siteId: Long?,
        status: EventStatus?,
        siteIds: List<Long>,
    ): List<IncidentRow> =
        kotlinJdslJpqlExecutor
            .findAllNotNull {
                selectIncidentRow()
                    .where(
                        and(
                            filterByFrom(from),
                            filterByTo(to),
                            filterBySiteId(siteId),
                            filterByStatus(status),
                            path(Site::id).`in`(siteIds),
                        ),
                    ).orderBy(path(Incident::id).desc())
            }

    override fun findEventListWithPaging(
        from: String?,
        to: String?,
        siteId: Long?,
        status: EventStatus?,
        level: ConditionLevel?,
        sensorType: SensorType?,
        sourceType: IncidentSourceType?,
        siteIds: List<Long>,
        size: Int,
        lastId: Long?,
        lastStatus: EventStatus?,
    ): List<IncidentRow> {
        val query =
            jpql {
                selectIncidentRow()
                    .where(
                        and(
                            filterByFrom(from),
                            filterByTo(to),
                            filterBySiteId(siteId),
                            filterByStatus(status),
                            level?.let { path(Incident::level).eq(it) },
                            sourceType?.let { path(Incident::sourceType).eq(it) },
                            sensorType?.let { path(EventHistory::objectId).eq(it.objectId) },
                            path(Site::id).`in`(siteIds),
                            cursorCondition(lastId, lastStatus),
                        ),
                    ).orderBy(
                        path(Incident::status).asc(),
                        path(Incident::id).desc(),
                    )
            }

        return entityManager
            .createQuery(query, renderContext)
            .apply { maxResults = size + 1 }
            .resultList
    }

    private fun Jpql.selectIncidentRow() =
        selectNew<IncidentRow>(
            path(Incident::id),
            path(Incident::sourceType),
            path(Incident::deviceId),
            path(Incident::deviceName),
            path(Incident::title),
            path(EventHistory::objectId),
            path(Incident::occurredAt),
            path(EventHistory::minValue),
            path(EventHistory::maxValue),
            path(Incident::status),
            path(EventHistory::eventName),
            path(EventHistory::fieldKey),
            path(Incident::guideMessage),
            path(Incident::longitude),
            path(Incident::latitude),
            path(Incident::updatedBy),
            path(Incident::updatedAt),
            path(EventHistory::value),
            path(Incident::level),
            path(Site::id),
            path(Site::name),
        ).from(
            entity(Incident::class),
            leftJoin(entity(EventHistory::class)).on(
                and(
                    path(Incident::sourceType).eq(IncidentSourceType.SENSOR),
                    path(Incident::sourceId).equal(path(EventHistory::id)),
                ),
            ),
            leftJoin(Incident::site),
        )

    private fun Jpql.cursorCondition(
        lastId: Long?,
        lastStatus: EventStatus?,
    ): Predicate? =
        if (lastId != null && lastStatus != null) {
            or(
                path(Incident::status).greaterThan(lastStatus),
                and(
                    path(Incident::status).eq(lastStatus),
                    path(Incident::id).lessThanOrEqualTo(lastId),
                ),
            )
        } else {
            null
        }

    private fun Jpql.filterByFrom(from: String?): Predicate? =
        from?.let { path(Incident::occurredAt).greaterThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) }

    private fun Jpql.filterByTo(to: String?): Predicate? =
        to?.let { path(Incident::occurredAt).lessThanOrEqualTo(DateTimeUtils.parseCompactDateTime(it)) }

    private fun Jpql.filterBySiteId(siteId: Long?): Predicate? = siteId?.let { path(Site::id).eq(it) }

    private fun Jpql.filterByStatus(status: EventStatus?): Predicate? = status?.let { path(Incident::status).eq(it) }
}
