package com.pluxity.aiot.action

import com.pluxity.aiot.event.EventStatusChangeNotifier
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
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
    private val eventStatusChangeNotifier: EventStatusChangeNotifier,
) {
    companion object {
        private const val ACTION_HISTORIES: String = "action-histories/"
    }

    private fun findEventHistoryById(id: Long): EventHistory =
        eventHistoryRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_HISTORY, id)

    private fun findActionHistoryById(
        eventId: Long,
        id: Long,
    ): ActionHistory =
        actionHistoryRepository.findByIdAndEventHistory(id, findEventHistoryById(eventId))
            ?: throw CustomException(ErrorCode.NOT_FOUND_ACTION_HISTORY, id)

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
        // 파일 처리를 배치로 개선
        requestDto.fileIds?.let { fileIds ->
            // 파일 업로드 finalize를 먼저 배치 처리
            fileIds.forEach { fileId ->
                fileService.finalizeUpload(fileId, "${ACTION_HISTORIES}${savedActionHistory.requiredId}/")
            }

            // ActionHistoryFile 엔티티들을 배치로 저장
            val actionHistoryFiles =
                fileIds.map { fileId ->
                    ActionHistoryFile(
                        actionHistory = savedActionHistory,
                        fileId = fileId,
                    )
                }
            actionHistoryFileRepository.saveAll(actionHistoryFiles)
        }
        eventHistory.changeStatus(EventStatus.RESOLVED)
        eventStatusChangeNotifier.notifyStatusChanged(eventHistory, eventId, EventStatus.RESOLVED.name)
        return savedActionHistory.requiredId
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

    @Transactional
    fun update(
        eventId: Long,
        id: Long,
        request: ActionHistoryRequest,
    ) {
        val actionHistory = findActionHistoryById(eventId, id)
        actionHistory.updateContent(request.content)
        val existIds = actionHistory.historyFiles.map { it.fileId }
        request.fileIds?.let {
            actionHistoryFileRepository.deleteByIdIn(existIds.minus(it.toSet()))
            val filesToAdd = it.minus(existIds.toSet())
            filesToAdd.forEach { fileId ->
                fileService.finalizeUpload(fileId, "${ACTION_HISTORIES}$id/")
            }
            val newActionHistoryFiles =
                filesToAdd.map { fileId ->
                    ActionHistoryFile(
                        actionHistory = actionHistory,
                        fileId = fileId,
                    )
                }
            actionHistoryFileRepository.saveAll(newActionHistoryFiles)
        }
    }

    @Transactional
    fun delete(
        eventId: Long,
        id: Long,
    ) {
        val actionHistory = findActionHistoryById(eventId, id)
        actionHistoryFileRepository.deleteAll(actionHistory.historyFiles)
        actionHistoryRepository.delete(actionHistory)
    }
}
