package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class UmsSmsSender(
    private val umsClient: UmsClient,
    private val umsProperties: UmsProperties,
) : SmsSender {
    override fun send(request: SmsSendRequest): SmsSendResult {
        SmsValidator.validate(request, umsProperties.senderNumber)?.let { reason ->
            log.warn { "문자 발송 요청이 유효하지 않아 호출하지 않습니다: $reason" }
            return SmsSendResult(UmsSendStat.NOT_SENT, failureReason = reason)
        }

        return try {
            umsClient.syncSend(request.title, request.message, request.targetNumber)
        } catch (e: Exception) {
            log.error(e) { "UMS 전송 요청 실패: ${e.message}" }
            SmsSendResult(UmsSendStat.NOT_SENT, failureReason = e.message)
        }
    }
}

/** 미연동 환경에서 실제 발송 없이 요청 내용만 남긴다 */
@Component
@ConditionalOnProperty("ums.enabled", havingValue = "false", matchIfMissing = true)
class LoggingSmsSender : SmsSender {
    override fun send(request: SmsSendRequest): SmsSendResult {
        log.info {
            "[UMS 미연동] 문자 발송 생략 - 대상: ${request.targetNumber}, 제목: ${request.title}, 내용: ${request.message}"
        }
        return SmsSendResult(UmsSendStat.NOT_SENT, failureReason = "UMS 미연동")
    }
}
