package com.pluxity.aiot.action

import com.pluxity.aiot.action.ActionHistory.ActionResult
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActionHistoryService(
    private val actionHistoryRepository: ActionHistoryRepository,
    private val eventHistoryRepository: EventHistoryRepository,
) {
    private fun findEventHistoryById(id: Long): EventHistory =
        eventHistoryRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_HISTORY, id)

    @Transactional
    fun save(requestDto: ActionHistoryRequest): Long {
        val eventHistory = findEventHistoryById(requestDto.eventHistoryId)

        val savedActionHistory =
            actionHistoryRepository.save(
                ActionHistory(
                    eventHistory = eventHistory,
                    deviceId = requestDto.deviceId,
                    eventName = requestDto.eventName,
                    content = requestDto.content,
                    actionType = ActionHistory.ActionType.valueOf(requestDto.actionType),
                    actionResult = ActionHistory.ActionResult.valueOf(requestDto.actionResult),
                    ignored = requestDto.ignored,
                    actedBy = requestDto.actedBy,
                ),
            )
        return savedActionHistory.id!!
    }

    @Transactional(readOnly = true)
    fun findAll(): List<ActionHistoryResponse> = actionHistoryRepository.findAll().map { it.toActionHistoryResponse() }

    @Transactional(readOnly = true)
    fun findByDeviceIdAndEventName(deviceId: String): List<ActionHistoryResponse> =
        actionHistoryRepository.findByDeviceId(deviceId).map { it.toActionHistoryResponse() }

    @Transactional(readOnly = true)
    fun findByEventHistory(eventHistoryId: Long): List<ActionHistoryResponse> {
        val eventHistory = findEventHistoryById(eventHistoryId)
        return actionHistoryRepository.findByEventHistory(eventHistory).map { it.toActionHistoryResponse() }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): ActionHistoryResponse {
        val actionHist =
            actionHistoryRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_ACTION_HISTORY, id)
        return actionHist.toActionHistoryResponse()
    }

    @Transactional
    fun createAutomaticAction(
        deviceId: String,
        eventName: String,
        eventHistory: EventHistory,
        actionResult: ActionResult,
        ignored: Boolean,
    ) {
        val actionHistory =
            ActionHistory(
                deviceId = deviceId,
                eventName = eventName,
                eventHistory = eventHistory,
                actionType = ActionHistory.ActionType.AUTOMATIC,
                actionResult = actionResult,
                ignored = ignored,
                content = "자동조치",
                actedBy = "SYSTEM",
            )

        actionHistoryRepository.save(actionHistory)
    }

    @Transactional
    fun createManualAction(
        deviceId: String,
        eventName: String,
        eventHistory: EventHistory,
        actionResult: ActionResult,
        ignored: Boolean,
        actedBy: String,
    ) {
        val actionHistory =
            ActionHistory(
                deviceId = deviceId,
                eventName = eventName,
                eventHistory = eventHistory,
                actionType = ActionHistory.ActionType.MANUAL,
                actionResult = actionResult,
                ignored = ignored,
                content = "수동조치",
                actedBy = actedBy,
            )

        actionHistoryRepository.save(actionHistory)
    }
}
