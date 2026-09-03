package com.pluxity.aiot.sms

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 문자 발송 이력의 트랜잭션 경계. 외부 DB 호출은 이 안에서 하지 않는다 */
@Service
@Transactional(readOnly = true)
class SmsHistoryService(
    private val smsHistoryRepository: SmsHistoryRepository,
) {
    fun findPending(batchSize: Int): List<SmsHistory> = smsHistoryRepository.findPendingResults(PageRequest.of(0, batchSize))

    @Transactional
    fun saveAll(histories: List<SmsHistory>): List<SmsHistory> = smsHistoryRepository.saveAll(histories)
}
