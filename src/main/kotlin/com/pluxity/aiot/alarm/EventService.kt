package com.pluxity.aiot.alarm

import com.pluxity.aiot.alarm.dto.EventResponse
import com.pluxity.aiot.alarm.dto.toEventResponse
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.entity.HistoryResult
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.SiteRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventService(
    private val eventHistoryRepository: EventHistoryRepository,
    private val siteRepository: SiteRepository,
) {
    fun findAll(
        from: String?,
        to: String?,
        siteId: Long?,
        result: HistoryResult?,
    ): List<EventResponse> {
        val siteList = siteRepository.findAllByOrderByCreatedAtDesc().mapNotNull { it.id }
        return eventHistoryRepository.findEventList(from, to, siteId, result, siteList).map { it.toEventResponse() }
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
}
