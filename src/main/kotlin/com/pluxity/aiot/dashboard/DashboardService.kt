package com.pluxity.aiot.dashboard

import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.jpql.JpqlRenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.event.dto.EventResponse
import com.pluxity.aiot.event.dto.toEventResponse
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.site.SiteRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val featureRepository: FeatureRepository,
    private val siteRepository: SiteRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    private val entityManager: EntityManager,
    private val renderContext: JpqlRenderContext,
) {
    fun getSensorSummary(): List<SensorSummary> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        return featureRepository
            .findAll {
                selectNew<SensorStatisticsRaw>(
                    path(Site::id),
                    path(Site::name),
                    count(path(Feature::id)),
                    count(
                        caseWhen(
                            path(Feature::eventStatus)
                                .eq("DISCONNECTED"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::eventStatus)
                                .notEqual("DISCONNECTED"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.TEMPERATURE_HUMIDITY.objectId}%"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.FIRE.objectId}%"),
                        ).then(1),
                    ),
                    count(
                        caseWhen(
                            path(Feature::objectId)
                                .like("${SensorType.DISPLACEMENT_GAUGE.objectId}%"),
                        ).then(1),
                    ),
                ).from(
                    entity(Feature::class),
                    join(Feature::site),
                ).where(
                    and(
                        path(Site::id).isNotNull(),
                        path(Site::id).`in`(siteIds),
                    ),
                ).groupBy(path(Site::id))
            }.filterNotNull()
            .map { it.toSummaryStatistics() }
    }

    fun getEventSummary(
        from: String?,
        to: String?,
    ): Map<EventStatus, List<EventResponse>> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        val eventList = eventHistoryRepository.findEventList(from = from, to = to, siteIds = siteIds)
        val events = mutableMapOf<EventStatus, List<EventResponse>>()
        EventStatus.entries.forEach { result ->
            events[result] = eventList.filter { it.status == result }.map { it.toEventResponse() }
        }
        return events
    }

    fun getActionHistories(): List<ActionHistorySummary> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        val query =
            jpql {
                selectNew<ActionHistoryRaw>(
                    path(Site::name),
                    path(Feature::name),
                    path(EventHistory::fieldKey),
                    path(EventHistory::status),
                    path(ActionHistory::createdAt),
                ).from(
                    entity(ActionHistory::class),
                    join(ActionHistory::eventHistory),
                    join(entity(Feature::class)).on(path(EventHistory::deviceId).equal(path(Feature::deviceId))),
                    join(Feature::site),
                ).where(
                    path(Site::id).`in`(siteIds),
                )
            }
        val histories =
            entityManager
                .createQuery(query, renderContext)
                .apply { maxResults = 10 }
                .resultList
                .filterNotNull()

        return histories.map { it.toHistorySummary() }
    }

    private data class SensorStatisticsRaw(
        val siteId: Long,
        val siteName: String,
        val totalCount: Long,
        val disconnectedCount: Long,
        val connectedCount: Long,
        val temperatureHumidityCount: Long,
        val fireCount: Long,
        val displacementCount: Long,
    )

    private fun SensorStatisticsRaw.toSummaryStatistics() =
        SensorSummary(
            siteId = this.siteId,
            siteName = this.siteName,
            totalSensors = this.totalCount,
            connectionStatus =
                ConnectionStatus(
                    connected = this.connectedCount,
                    disconnected = this.disconnectedCount,
                ),
            sensorTypeStatus =
                SensorTypeStatus(
                    temperatureHumidity = this.temperatureHumidityCount,
                    fire = this.fireCount,
                    displacement = this.displacementCount,
                ),
        )

    private data class ActionHistoryRaw(
        val siteName: String,
        val deviceName: String,
        val fieldKey: String,
        val status: EventStatus,
        val createdAt: LocalDateTime,
    )

    private fun ActionHistoryRaw.toHistorySummary() =
        ActionHistorySummary(
            siteName = this.siteName,
            deviceName = this.deviceName,
            eventName = "${DeviceProfileEnum.getDescriptionByFieldKey(this.fieldKey)} 오류",
            status = this.status.name,
            createdAt = this.createdAt.toString(),
        )
}
