package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.UmsSendResultRow
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

/**
 * sp_syncSend는 접수까지만 하므로, 확정되지 않은 이력을 주기적으로 조회해 갱신한다.
 * 외부 조회는 트랜잭션 밖에서 하고 조회 결과만 [SmsHistoryService]에 넘긴다.
 */
@Component
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class SmsResultSyncService(
    private val umsClient: UmsClient,
    private val smsHistoryService: SmsHistoryService,
    private val umsProperties: UmsProperties,
) {
    fun syncPendingResults(): Int {
        val cutoff = LocalDateTime.now().minusHours(umsProperties.resultPollCutoffHours)
        val pending = smsHistoryService.findPending(umsProperties.resultPollBatchSize, cutoff)
        if (pending.isEmpty()) return 0

        var consecutiveFailures = 0
        // 조회에 실패해도 시각을 남겨야 계속 실패하는 건이 뒤쪽 건의 조회를 막지 않는다
        val checked = LinkedHashMap<Long, UmsSendResultRow?>()

        for (target in pending) {
            checked[target.id] =
                try {
                    umsClient.findSendResult(target.clidx).also { consecutiveFailures = 0 }
                } catch (e: Exception) {
                    consecutiveFailures++
                    log.error(e) { "문자 발송 결과 조회 실패 (clidx=${target.clidx}): ${e.message}" }
                    null
                }

            // UMS가 응답하지 않는 상황에서 남은 건까지 순차 대기하면 스케줄러 스레드가 오래 묶인다
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                log.warn { "결과 조회가 연속 ${consecutiveFailures}회 실패해 이번 주기를 중단합니다" }
                break
            }
        }

        val confirmed = smsHistoryService.applyResults(checked)
        log.info { "문자 발송 결과 동기화: 대상=${pending.size}, 조회=${checked.size}, 확정=$confirmed" }
        return confirmed
    }

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
