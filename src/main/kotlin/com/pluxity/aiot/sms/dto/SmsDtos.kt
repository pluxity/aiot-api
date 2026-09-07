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

/** 엔티티를 그대로 넘기지 않도록 발송 결과만 추린다 */
data class SmsDispatchResult(
    val historyId: Long?,
    val targetNumber: String,
    val stat: UmsSendStat,
    val clidx: Long?,
    val failureReason: String?,
)

data class UmsSendResultRow(
    val resultCode: Int?,
    val statusCode: Int?,
    val errorCode: String?,
    val messageType: String?,
    val completedAt: LocalDateTime?,
)

/** 결과 조회에 필요한 값만 읽어 엔티티를 detached 상태로 들고 다니지 않는다 */
data class PendingSmsResult(
    val id: Long,
    val clidx: Long,
)
