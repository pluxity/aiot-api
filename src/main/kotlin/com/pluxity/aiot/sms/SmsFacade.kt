package com.pluxity.aiot.sms

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsCallOutcome
import com.pluxity.aiot.sms.dto.SmsDispatchOutcome
import com.pluxity.aiot.sms.dto.SmsDispatchResult
import com.pluxity.aiot.sms.dto.SmsSendRequest
import com.pluxity.aiot.sms.dto.SmsSendResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronizationManager

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
     * 트랜잭션 안에서 부르면 안 된다. 수신자 수에 비례하는 외부 DB 왕복이 일어나는 동안
     * 바깥 트랜잭션이 커넥션을 붙잡고, [SmsHistoryService.saveAll]의 REQUIRES_NEW가
     * 같은 풀에서 두 번째 커넥션을 잡아 동시 호출 몇 건만으로 풀이 마른다.
     * 이벤트 처리 중 발송해야 하면 `@TransactionalEventListener(phase = AFTER_COMMIT)`으로 옮긴다.
     */
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsDispatchResult> {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "SmsFacade.send()는 트랜잭션 밖에서 호출해야 합니다"
        }
        SmsValidator.validateContent(title, message)?.let { reason ->
            throw CustomException(ErrorCode.SMS_INVALID_CONTENT, reason)
        }

        val dispatched = mutableListOf<Dispatched>()
        return try {
            dispatch(title, message, targetNumbers, dispatched)
            persist(dispatched)
        } catch (e: Exception) {
            // 이미 나간 문자의 이력은 남겨야 하므로, 중간에 끊겨도 모아둔 만큼은 저장한다
            runCatching { persist(dispatched) }
                .onFailure { log.error(it) { "중단된 발송의 이력 저장 실패" } }
            throw e
        }
    }

    private fun dispatch(
        title: String,
        message: String,
        targetNumbers: List<String>,
        dispatched: MutableList<Dispatched>,
    ) {
        var consecutiveFailures = 0
        var aborted = false

        for (targetNumber in distinctRecipients(targetNumbers)) {
            // UMS가 응답하지 않으면 수신자 수만큼 타임아웃을 누적해 호출 스레드가 오래 묶인다
            if (aborted) {
                val skipped = SmsSendResult(UmsSendStat.NOT_SENT, failureReason = ABORT_REASON)
                dispatched += Dispatched(history(title, message, targetNumber, skipped), SmsDispatchOutcome.ABORTED)
                continue
            }

            val result = smsSender.send(SmsSendRequest(title, message, targetNumber))
            // 호출하지 않고 끝난 건은 UMS 상태를 말해주지 않으므로 카운터를 건드리지 않는다
            when (result.callOutcome) {
                SmsCallOutcome.CALL_FAILED -> consecutiveFailures++
                SmsCallOutcome.CALLED -> consecutiveFailures = 0
                SmsCallOutcome.NOT_CALLED -> Unit
            }

            if (result.stat != UmsSendStat.SUCCESS) {
                log.warn {
                    "문자 발송 실패 - 대상: ${SmsValidator.maskNumber(targetNumber)}, " +
                        "상태: ${result.stat.description}, 사유: ${result.failureReason}"
                }
            }
            dispatched += Dispatched(history(title, message, targetNumber, result), outcomeOf(result))

            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                log.warn { "발송 호출이 연속 ${consecutiveFailures}회 실패해 남은 대상은 시도하지 않습니다" }
                aborted = true
            }
        }
    }

    private fun persist(dispatched: List<Dispatched>): List<SmsDispatchResult> {
        if (dispatched.isEmpty()) return emptyList()
        // saveAll이 채워 준 id를 쓰려고 저장된 엔티티 쪽을 읽는다. outcome은 엔티티에 없는 값이다
        val saved = smsHistoryService.saveAll(dispatched.map { it.history })
        return saved.zip(dispatched) { history, (_, outcome) ->
            SmsDispatchResult(
                historyId = history.id,
                targetNumber = history.targetNumber,
                outcome = outcome,
                stat = history.stat,
                clidx = history.clidx,
                failureReason = history.failureReason,
            )
        }
    }

    /** 이력과 판정을 함께 들고 다녀 길이가 어긋날 수 없게 한다. outcome은 저장되지 않는 값이다 */
    private data class Dispatched(
        val history: SmsHistory,
        val outcome: SmsDispatchOutcome,
    )

    private fun outcomeOf(result: SmsSendResult): SmsDispatchOutcome =
        when (result.callOutcome) {
            SmsCallOutcome.NOT_CALLED -> SmsDispatchOutcome.NOT_CALLED
            SmsCallOutcome.CALL_FAILED -> SmsDispatchOutcome.CALL_FAILED
            SmsCallOutcome.CALLED ->
                if (result.stat == UmsSendStat.SUCCESS) SmsDispatchOutcome.ACCEPTED else SmsDispatchOutcome.REJECTED
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
