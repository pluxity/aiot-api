package com.pluxity.aiot.sms.dto

import com.pluxity.aiot.sms.UmsSendStat
import java.time.LocalDateTime

data class SmsSendRequest(
    val title: String,
    val message: String,
    val targetNumber: String,
)

data class SmsSendResult(
    val stat: UmsSendStat,
    /** 프로시저가 돌려준 원본 상태 코드. 정의되지 않은 값도 그대로 보관한다 */
    val statCode: Int? = null,
    /** 발신고유번호. 요청이 실패하면 null */
    val clidx: Long? = null,
    val failureReason: String? = null,
)

data class UmsSendResultRow(
    val resultCode: Int?,
    val statusCode: Int?,
    val errorCode: String?,
    val messageType: String?,
    val completedAt: LocalDateTime?,
)
