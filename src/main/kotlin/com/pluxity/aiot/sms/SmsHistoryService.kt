package com.pluxity.aiot.sms

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** 문자 발송 이력의 트랜잭션 경계. 외부 DB 호출은 이 안에서 하지 않는다 */
@Service
@Transactional(readOnly = true)
class SmsHistoryService(
    private val smsHistoryRepository: SmsHistoryRepository,
) {
    fun findPending(
        batchSize: Int,
        cutoff: LocalDateTime,
    ): List<SmsHistory> = smsHistoryRepository.findPendingResults(cutoff, PageRequest.of(0, batchSize))

    /** 이미 발송된 문자의 이력은 바깥 트랜잭션이 롤백돼도 남아야 하므로 별도 트랜잭션으로 분리한다 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveAll(histories: List<SmsHistory>): List<SmsHistory> = smsHistoryRepository.saveAll(histories)
}
