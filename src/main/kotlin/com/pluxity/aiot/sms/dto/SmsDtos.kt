package com.pluxity.aiot.sms.dto

import com.pluxity.aiot.sms.UmsSendStat
import java.time.LocalDateTime

data class SmsSendRequest(
    val title: String,
    val message: String,
    val targetNumber: String,
)

/** 외부 호출이 실제로 일어났는지 구분한다. 연속 실패를 셀 때 호출하지 않은 건과 섞이면 안 된다 */
enum class SmsCallOutcome {
    /** 프로시저 응답을 받음. 프로시저가 거절한 경우도 호출 자체는 성공이다 */
    CALLED,

    /** 검증 실패나 미연동이라 호출하지 않음 */
    NOT_CALLED,

    /** 호출 자체가 실패함 */
    CALL_FAILED,
}

data class SmsSendResult(
    val stat: UmsSendStat,
    /** 프로시저가 돌려준 원본 상태 코드. 정의되지 않은 값도 그대로 보관한다 */
    val statCode: Int? = null,
    /** 발신고유번호. 요청이 실패하면 null */
    val clidx: Long? = null,
    val failureReason: String? = null,
    val callOutcome: SmsCallOutcome = SmsCallOutcome.CALLED,
)

/** 호출자가 재시도 여부를 문자열 비교 없이 판단할 수 있도록 결과를 구분한다 */
enum class SmsDispatchOutcome {
    /** UMS가 접수함 */
    ACCEPTED,

    /** 호출은 됐으나 프로시저가 거절함. 같은 값으로 재시도해도 결과가 같다 */
    REJECTED,

    /** 검증 실패 또는 미연동이라 호출하지 않음 */
    NOT_CALLED,

    /** 호출 자체가 실패함. 재시도 가치가 있다 */
    CALL_FAILED,

    /** 앞선 건이 연속 실패해 시도하지 않음. 재시도 가치가 있다 */
    ABORTED,
}

/** 엔티티를 그대로 넘기지 않도록 발송 결과만 추린다 */
data class SmsDispatchResult(
    val historyId: Long?,
    val targetNumber: String,
    val outcome: SmsDispatchOutcome,
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
