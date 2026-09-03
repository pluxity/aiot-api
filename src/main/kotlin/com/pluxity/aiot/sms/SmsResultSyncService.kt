package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * sp_syncSend는 접수까지만 하므로, 확정되지 않은 이력을 주기적으로 조회해 갱신한다.
 * 외부 조회는 트랜잭션 밖에서 하고 갱신된 엔티티만 [SmsHistoryService]에 넘긴다.
 */
@Component
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class SmsResultSyncService(
    private val umsClient: UmsClient,
    private val smsHistoryService: SmsHistoryService,
    private val umsProperties: UmsProperties,
) {
    fun syncPendingResults(): Int {
        val pending = smsHistoryService.findPending(umsProperties.resultPollBatchSize)
        if (pending.isEmpty()) return 0

        var updated = 0
        for (history in pending) {
            val clidx = history.clidx ?: continue
            val row =
                try {
                    umsClient.findSendResult(clidx)
                } catch (e: Exception) {
                    log.error(e) { "문자 발송 결과 조회 실패 (clidx=$clidx): ${e.message}" }
                    null
                }
            // 조회에 실패해도 시각을 남겨야 계속 실패하는 건이 뒤쪽 건의 조회를 막지 않는다
            history.markResultChecked(row)
            if (row != null) updated++
        }

        smsHistoryService.saveAll(pending)
        log.info { "문자 발송 결과 동기화: 대상=${pending.size}, 갱신=$updated" }
        return updated
    }
}
