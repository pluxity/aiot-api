package com.pluxity.aiot.dashboard

import com.pluxity.aiot.event.dto.EventResponse
import com.pluxity.aiot.event.dto.toEventResponse
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.site.SiteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val featureRepository: FeatureRepository,
    private val siteRepository: SiteRepository,
    private val eventHistoryRepository: EventHistoryRepository,
) {
    fun getSensorSummary(): List<SensorSummary> {
        val siteIds = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        return featureRepository
            .findSensorStatisticsBySiteIds(siteIds)
            .map { it.toSensorSummary() }
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
}
