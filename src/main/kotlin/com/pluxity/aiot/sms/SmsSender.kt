package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.SmsSendRequest
import com.pluxity.aiot.sms.dto.SmsSendResult

/** UMS 연동이 꺼져 있으면 [LoggingSmsSender]가 대신 동작한다 */
interface SmsSender {
    fun send(request: SmsSendRequest): SmsSendResult
}
