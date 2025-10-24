package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.event.condition.dto.EventConditionBatchRequest
import com.pluxity.aiot.system.event.condition.dto.EventConditionResponse
import com.pluxity.aiot.system.event.condition.dto.toEventConditionResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class EventConditionService(
    private val eventConditionRepository: EventConditionRepository,
) {
    @Transactional(readOnly = true)
    fun findAllByObjectId(objectId: String): List<EventConditionResponse> {
        log.info { "EventCondition 목록 조회 - objectId: $objectId" }
        return eventConditionRepository
            .findAllByObjectId(objectId)
            .map { it.toEventConditionResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): EventConditionResponse {
        log.info { "EventCondition 조회 - id: $id" }
        val condition =
            eventConditionRepository.findByIdOrNull(id)
                ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_CONDITION, id)
        return condition.toEventConditionResponse()
    }

    @Transactional
    fun createBatch(request: EventConditionBatchRequest): List<Long> {
        log.info { "EventCondition 일괄 생성 시작 - objectId: ${request.objectId}, count: ${request.conditions.size}" }

        // 조건 생성
        val newConditions =
            request.conditions.map { itemRequest ->
                EventCondition(
                    objectId = request.objectId,
                    fieldKey = itemRequest.fieldKey,
                    isActivate = itemRequest.isActivate,
                    level = itemRequest.level,
                    conditionType = itemRequest.conditionType,
                    operator = itemRequest.operator,
                    thresholdValue = itemRequest.thresholdValue,
                    leftValue = itemRequest.leftValue,
                    rightValue = itemRequest.rightValue,
                    booleanValue = itemRequest.booleanValue,
                    notificationEnabled = itemRequest.notificationEnabled,
                    order = itemRequest.order,
                )
            }

        validateRangeOverlap(newConditions)

        // 저장
        val savedConditions =
            newConditions.map { condition ->
                eventConditionRepository.save(condition)
            }

        log.info { "EventCondition 일괄 생성 완료 - objectId: ${request.objectId}, count: ${savedConditions.size}" }

        return savedConditions.map { it.id!! }
    }

    @Transactional
    fun updateBatch(request: EventConditionBatchRequest): List<EventConditionResponse> {
        log.info { "EventCondition 일괄 수정 시작 - objectId: ${request.objectId}, count: ${request.conditions.size}" }

        // 기존 조건들 삭제
        eventConditionRepository.deleteAllByObjectId(request.objectId)

        // 새로운 조건들 생성
        val newConditions =
            request.conditions.map { itemRequest ->
                EventCondition(
                    objectId = request.objectId,
                    fieldKey = itemRequest.fieldKey,
                    isActivate = itemRequest.isActivate,
                    level = itemRequest.level,
                    conditionType = itemRequest.conditionType,
                    operator = itemRequest.operator,
                    thresholdValue = itemRequest.thresholdValue,
                    leftValue = itemRequest.leftValue,
                    rightValue = itemRequest.rightValue,
                    booleanValue = itemRequest.booleanValue,
                    notificationEnabled = itemRequest.notificationEnabled,
                    order = itemRequest.order,
                )
            }

        validateRangeOverlap(newConditions)

        // 저장
        val savedConditions =
            newConditions.map { condition ->
                eventConditionRepository.save(condition)
            }

        log.info { "EventCondition 일괄 수정 완료 - objectId: ${request.objectId}, count: ${savedConditions.size}" }

        return savedConditions.map { it.toEventConditionResponse() }
    }

    @Transactional
    fun delete(id: Long) {
        log.info { "EventCondition 삭제 시작 - id: $id" }

        val condition =
            eventConditionRepository.findByIdOrNull(id)
                ?: throw CustomException(ErrorCode.NOT_FOUND_EVENT_CONDITION, id)

        eventConditionRepository.delete(condition)

        log.info { "EventCondition 삭제 완료 - id: $id" }
    }

    @Transactional
    fun deleteAllByObjectId(objectId: String): Int {
        log.info { "EventCondition 일괄 삭제 시작 - objectId: $objectId" }
        val deletedCount = eventConditionRepository.deleteAllByObjectId(objectId)
        log.info { "EventCondition 일괄 삭제 완료 - objectId: $objectId, 삭제 수: $deletedCount" }
        return deletedCount
    }

    private fun validateRangeOverlap(conditions: List<EventCondition>) {
        // 같은 fieldKey끼리 범위 중복 확인
        for (i in conditions.indices) {
            for (j in i + 1 until conditions.size) {
                val condition1 = conditions[i]
                val condition2 = conditions[j]

                if (condition1.hasRangeOverlap(condition2)) {
                    val range1 = condition1.getActualRange()!!
                    val range2 = condition2.getActualRange()!!

                    throw IllegalArgumentException(
                        "조건 범위가 겹칩니다. " +
                            "fieldKey='${condition1.fieldKey}', " +
                            "level1=${condition1.level} (${range1.first}~${range1.second}), " +
                            "level2=${condition2.level} (${range2.first}~${range2.second})",
                    )
                }
            }
        }
    }
}
