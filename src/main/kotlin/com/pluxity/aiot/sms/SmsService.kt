package com.pluxity.aiot.sms

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsSendRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * 외부 DB 호출을 트랜잭션 밖에서 끝내고 이력만 저장한다.
 * 발송 도중 저장이 실패해 이미 나간 문자의 이력까지 롤백되면 안 된다.
 */
@Service
class SmsService(
    private val smsSender: SmsSender,
    private val smsHistoryRepository: SmsHistoryRepository,
    private val umsProperties: UmsProperties,
) {
    /** 한 건이 실패해도 나머지 발송은 계속한다 */
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsHistory> {
        SmsValidator.validateContent(title, message)?.let { reason ->
            throw CustomException(ErrorCode.SMS_INVALID_CONTENT, reason)
        }

        val histories =
            distinctRecipients(targetNumbers).map { targetNumber ->
                val result = smsSender.send(SmsSendRequest(title, message, targetNumber))

                if (result.stat != UmsSendStat.SUCCESS) {
                    log.warn {
                        "문자 발송 실패 - 대상: ${SmsValidator.maskNumber(targetNumber)}, " +
                            "상태: ${result.stat.description}, 사유: ${result.failureReason}"
                    }
                }

                SmsHistory(
                    targetNumber = targetNumber,
                    senderNumber = umsProperties.senderNumber.takeIf { it.isNotBlank() },
                    title = title,
                    message = message,
                    stat = result.stat,
                    statCode = result.statCode,
                    clidx = result.clidx,
                    failureReason = SmsHistory.truncateFailureReason(result.failureReason),
                )
            }

        return smsHistoryRepository.saveAll(histories)
    }

    /** 표기가 여럿이면 유효한 것을 대표로 골라, 잘못된 표기 때문에 유효한 번호가 빠지지 않게 한다 */
    private fun distinctRecipients(targetNumbers: List<String>): List<String> =
        targetNumbers
            .groupBy(SmsValidator::normalizeNumber)
            .map { (_, sameNumbers) -> sameNumbers.firstOrNull(SmsValidator::isValidNumber) ?: sameNumbers.first() }
}
