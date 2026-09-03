package com.pluxity.aiot.sms

/**
 * 문자 발송 포트. UMS 연동이 꺼져 있으면 [LoggingSmsSender]가 대신 동작한다.
 */
interface SmsSender {
    fun send(request: SmsSendRequest): SmsSendResult
}

data class SmsSendRequest(
    val title: String,
    val message: String,
    val targetNumber: String,
)

data class SmsSendResult(
    val stat: UmsSendStat,
    /** 발신고유번호. 결과 조회에 사용한다. 요청이 실패하면 null */
    val clidx: Long? = null,
    val failureReason: String? = null,
)
