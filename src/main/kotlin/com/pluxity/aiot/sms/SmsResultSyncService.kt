package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

/** sp_syncSend는 접수까지만 하므로, 확정되지 않은 이력을 주기적으로 조회해 갱신한다 */
@Service
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class SmsResultSyncService(
    private val umsClient: UmsClient,
    private val smsHistoryRepository: SmsHistoryRepository,
    private val umsProperties: UmsProperties,
) {
    @Transactional
    fun syncPendingResults(): Int {
        val pending =
            smsHistoryRepository.findPendingResults(PageRequest.of(0, umsProperties.resultPollBatchSize))
        if (pending.isEmpty()) return 0

        var updated = 0
        for (history in pending) {
            val clidx = history.clidx ?: continue
            try {
                val row = umsClient.findSendResult(clidx)
                history.markResultChecked(row)
                if (row != null) updated++
            } catch (e: Exception) {
                log.error(e) { "문자 발송 결과 조회 실패 (clidx=$clidx): ${e.message}" }
            }
        }

        log.info { "문자 발송 결과 동기화: 대상=${pending.size}, 갱신=$updated" }
        return updated
    }
}
