package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class SmsService(
    private val smsSender: SmsSender,
    private val smsHistoryRepository: SmsHistoryRepository,
    private val umsProperties: UmsProperties,
) {
    /**
     * 대상 번호마다 개별 발송하고 결과를 이력으로 남긴다.
     * 한 건이 실패해도 나머지 발송은 계속한다.
     */
    @Transactional
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsHistory> =
        // 010-1234-5678과 01012345678은 같은 대상이므로 숫자만 뽑아 비교하고, 발송은 원본 표기로 한다
        targetNumbers.distinctBy(SmsValidator::normalizeNumber).map { targetNumber ->
            val request = SmsSendRequest(title, message, targetNumber)
            val result = smsSender.send(request)

            if (result.stat != UmsSendStat.SUCCESS) {
                log.warn { "문자 발송 실패 - 대상: $targetNumber, 상태: ${result.stat.description}, 사유: ${result.failureReason}" }
            }

            smsHistoryRepository.save(
                SmsHistory(
                    targetNumber = targetNumber,
                    senderNumber = umsProperties.senderNumber.takeIf { it.isNotBlank() },
                    title = title,
                    message = message,
                    stat = result.stat,
                    clidx = result.clidx,
                    failureReason = result.failureReason,
                ),
            )
        }
}
