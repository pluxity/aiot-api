package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.PendingSmsResult
import com.pluxity.aiot.sms.dto.UmsSendResultRow
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
    ): List<PendingSmsResult> = smsHistoryRepository.findPendingResults(cutoff, PageRequest.of(0, batchSize))

    /** 이미 발송된 문자의 이력은 바깥 트랜잭션이 롤백돼도 남아야 하므로 별도 트랜잭션으로 분리한다 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun saveAll(histories: List<SmsHistory>): List<SmsHistory> = smsHistoryRepository.saveAll(histories)

    /**
     * detached 엔티티를 merge하면 건마다 SELECT가 한 번씩 더 나가므로,
     * 이 트랜잭션 안에서 한 번에 조회해 변경 감지로 갱신한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyResults(results: Map<Long, UmsSendResultRow?>): Int {
        if (results.isEmpty()) return 0
        return smsHistoryRepository
            .findAllById(results.keys)
            .count { it.markResultChecked(results[it.id]) }
    }
}
