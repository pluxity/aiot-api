package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.SmsSendRequest
import com.pluxity.aiot.sms.dto.SmsSendResult

/**
 * UMS 연동이 꺼져 있으면 [LoggingSmsSender]가 대신 동작한다.
 *
 * 구현은 예외를 던지지 않고 실패를 결과로 돌려준다. 한 건이 던지면 앞서 이미 나간
 * 문자의 이력까지 저장되지 못한다.
 */
interface SmsSender {
    fun send(request: SmsSendRequest): SmsSendResult
}
