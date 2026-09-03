package com.pluxity.aiot.sms

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.UmsProperties
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
     *
     * 동기 `@TransactionalEventListener(AFTER_COMMIT)`도 안 된다. 커밋은 끝났어도 커넥션 반납은
     * 그 뒤(cleanupAfterCompletion)라 여전히 붙잡고 있고, 아래 검사도 통과하지 못한다.
     * 이벤트 처리 중 발송해야 하면 `@Async`를 함께 걸어 다른 스레드로 넘긴다.
     */
    fun send(
        title: String,
        message: String,
        targetNumbers: List<String>,
    ): List<SmsDispatchResult> {
        check(!TransactionSynchronizationManager.isActualTransactionActive()) {
            "SmsFacade.send()는 트랜잭션 밖에서 호출해야 합니다. " +
                "이벤트 처리 중이라면 @TransactionalEventListener(AFTER_COMMIT)에 @Async를 함께 거세요"
        }
        SmsValidator.validateContent(title, message)?.let { reason ->
            throw CustomException(ErrorCode.SMS_INVALID_CONTENT, reason)
        }

        val histories =
            targetNumbers
                .distinctBy { SmsValidator.normalizeNumber(it).ifBlank { it } }
                .map { targetNumber ->
                    val result = smsSender.send(SmsSendRequest(title, message, targetNumber))
                    if (result.stat != UmsSendStat.SUCCESS) {
                        log.warn {
                            "문자 발송 실패 - 대상: ${SmsValidator.maskNumber(targetNumber)}, " +
                                "상태: ${result.stat.description}, 사유: ${result.failureReason}"
                        }
                    }
                    history(title, message, targetNumber, result)
                }

        return try {
            smsHistoryService.saveAll(histories).map {
                SmsDispatchResult(it.id, it.targetNumber, it.stat, it.clidx, it.failureReason)
            }
        } catch (e: Exception) {
            // 발송은 이미 끝났다. 호출자가 발송 실패로 오해하고 재시도하면 문자가 중복된다
            log.error(e) { "문자 발송은 완료됐으나 이력 저장에 실패했습니다" }
            throw CustomException(ErrorCode.SMS_HISTORY_SAVE_FAILED, e.message ?: "알 수 없는 오류")
        }
    }

    private fun history(
        title: String,
        message: String,
        targetNumber: String,
        result: SmsSendResult,
    ) = SmsHistory(
        targetNumber = SmsHistory.truncateNumber(targetNumber),
        senderNumber = SmsHistory.truncateNumber(umsProperties.senderNumber).takeIf { it.isNotBlank() },
        title = title,
        message = message,
        stat = result.stat,
        statCode = result.statCode,
        clidx = result.clidx,
        failureReason = SmsHistory.truncateFailureReason(result.failureReason),
    )
}
