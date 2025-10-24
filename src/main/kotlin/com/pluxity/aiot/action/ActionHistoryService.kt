package com.pluxity.aiot.action

import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.file.extensions.getFileMapById
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActionHistoryService(
    private val actionHistoryRepository: ActionHistoryRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryFileRepository: ActionHistoryFileRepository,
    private val fileService: FileService,
) {
    companion object {
        private const val ACTION_HISTORIES: String = "action-histories/"
    }

    private fun findEventHistoryById(id: Long): EventHistory =
        eventHistoryRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_HISTORY, id)

    @Transactional
    fun save(
        eventId: Long,
        requestDto: ActionHistoryRequest,
    ): Long {
        val eventHistory = findEventHistoryById(eventId)

        val savedActionHistory =
            actionHistoryRepository.save(
                ActionHistory(
                    eventHistory = eventHistory,
                    content = requestDto.content,
                ),
            )
        requestDto.fileIds?.forEach { fileId ->
            fileService.finalizeUpload(fileId, "${ACTION_HISTORIES}${savedActionHistory.id}")
            actionHistoryFileRepository.save(
                ActionHistoryFile(
                    actionHistory = savedActionHistory,
                    fileId = fileId,
                ),
            )
        }
        return savedActionHistory.id!!
    }

    @Transactional(readOnly = true)
    fun findAll(eventId: Long): List<ActionHistoryResponse> {
        val eventHistory = findEventHistoryById(eventId)
        val histories = actionHistoryRepository.findByEventHistory(eventHistory)
        if (histories.isEmpty()) return emptyList()

        val historyFiles = histories.flatMap { it.historyFiles }
        val fileMap = fileService.getFileMapById(historyFiles) { it.fileId }
        return histories.map { it.toActionHistoryResponse(fileMap) }
    }
}
