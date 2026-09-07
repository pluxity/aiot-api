package com.pluxity.aiot.sms

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class SmsResultScheduler(
    private val smsResultSyncService: SmsResultSyncService,
) {
    @Scheduled(fixedDelayString = $$"${ums.result-poll-interval-seconds:60}s")
    fun syncResults() {
        try {
            smsResultSyncService.syncPendingResults()
        } catch (e: Exception) {
            log.error(e) { "문자 발송 결과 동기화 중 오류: ${e.message}" }
        }
    }
}
