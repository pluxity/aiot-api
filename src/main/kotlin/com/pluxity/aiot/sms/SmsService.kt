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
    /** 한 건이 실패해도 나머지 발송은 계속한다 */
    @Transactional
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsHistory> {
        // 길이 초과 값이 이력 저장에서 걸리면 일부는 발송된 채로 트랜잭션이 롤백된다
        SmsValidator.validateContent(title, message)?.let { reason ->
            log.warn { "제목·내용이 유효하지 않아 발송하지 않습니다: $reason" }
            return emptyList()
        }

        return distinctRecipients(targetNumbers).map { targetNumber ->
            val result = smsSender.send(SmsSendRequest(title, message, targetNumber))

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

    /** 표기가 여럿이면 유효한 것을 대표로 골라, 잘못된 표기 때문에 유효한 번호가 빠지지 않게 한다 */
    private fun distinctRecipients(targetNumbers: List<String>): List<String> =
        targetNumbers
            .groupBy(SmsValidator::normalizeNumber)
            .map { (_, sameNumbers) -> sameNumbers.firstOrNull(SmsValidator::isValidNumber) ?: sameNumbers.first() }
}
