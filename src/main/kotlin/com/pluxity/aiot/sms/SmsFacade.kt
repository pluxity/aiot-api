package com.pluxity.aiot.sms

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsDispatchResult
import com.pluxity.aiot.sms.dto.SmsSendRequest
import com.pluxity.aiot.sms.dto.SmsSendResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 외부 발송을 트랜잭션 밖에서 끝내고 이력만 [SmsHistoryService]에 넘긴다.
 * 발송 도중 저장이 실패해 이미 나간 문자의 이력까지 롤백되면 안 된다.
 */
@Component
class SmsFacade(
    private val smsSender: SmsSender,
    private val smsHistoryService: SmsHistoryService,
    private val umsProperties: UmsProperties,
) {
    /**
     * 한 건이 실패해도 나머지 발송은 계속한다.
     *
     * 호출자는 이 메서드를 트랜잭션 안에서 부르면 안 된다. 수신자 수에 비례하는 외부 DB 왕복이
     * 일어나므로 바깥 트랜잭션이 그동안 애플리케이션 DB 커넥션을 붙잡는다.
     * 이력은 [SmsHistoryService.saveAll]이 별도 트랜잭션으로 남긴다.
     */
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsDispatchResult> {
        SmsValidator.validateContent(title, message)?.let { reason ->
            throw CustomException(ErrorCode.SMS_INVALID_CONTENT, reason)
        }

        val histories = mutableListOf<SmsHistory>()
        var consecutiveFailures = 0
        var aborted = false

        for (targetNumber in distinctRecipients(targetNumbers)) {
            // UMS가 응답하지 않으면 수신자 수만큼 타임아웃을 누적해 호출 스레드가 오래 묶인다
            if (aborted) {
                histories += history(title, message, targetNumber, SmsSendResult(UmsSendStat.NOT_SENT, failureReason = ABORT_REASON))
                continue
            }

            val result = smsSender.send(SmsSendRequest(title, message, targetNumber))
            consecutiveFailures = if (result.callFailed) consecutiveFailures + 1 else 0

            if (result.stat != UmsSendStat.SUCCESS) {
                log.warn {
                    "문자 발송 실패 - 대상: ${SmsValidator.maskNumber(targetNumber)}, " +
                        "상태: ${result.stat.description}, 사유: ${result.failureReason}"
                }
            }
            histories += history(title, message, targetNumber, result)

            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                log.warn { "발송 호출이 연속 ${consecutiveFailures}회 실패해 남은 대상은 시도하지 않습니다" }
                aborted = true
            }
        }

        return smsHistoryService.saveAll(histories).map {
            SmsDispatchResult(
                historyId = it.id,
                targetNumber = it.targetNumber,
                stat = it.stat,
                clidx = it.clidx,
                failureReason = it.failureReason,
            )
        }
    }

    private fun history(
        title: String,
        message: String,
        targetNumber: String,
        result: SmsSendResult,
    ) = SmsHistory(
        targetNumber = SmsHistory.truncateNumber(targetNumber),
        senderNumber = umsProperties.senderNumber.takeIf { it.isNotBlank() },
        title = title,
        message = message,
        stat = result.stat,
        statCode = result.statCode,
        clidx = result.clidx,
        failureReason = SmsHistory.truncateFailureReason(result.failureReason),
    )

    /**
     * 표기가 여럿이면 유효한 것을 대표로 골라, 잘못된 표기 때문에 유효한 번호가 빠지지 않게 한다.
     * 숫자가 하나도 없는 값은 서로 다른 값끼리 묶이지 않도록 원문을 키로 쓴다.
     */
    private fun distinctRecipients(targetNumbers: List<String>): List<String> =
        targetNumbers
            .groupBy { SmsValidator.normalizeNumber(it).ifBlank { it } }
            .map { (_, sameNumbers) -> sameNumbers.firstOrNull(SmsValidator::isValidNumber) ?: sameNumbers.first() }

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 3
        private const val ABORT_REASON = "이전 발송이 연속 실패해 시도하지 않음"
    }
}
