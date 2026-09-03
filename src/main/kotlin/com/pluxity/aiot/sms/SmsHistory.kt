package com.pluxity.aiot.sms

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.sms.dto.UmsSendResultRow
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(indexes = [Index(columnList = "clidx"), Index(columnList = "created_at")])
class SmsHistory(
    @Column(length = MAX_NUMBER_LENGTH, nullable = false)
    var targetNumber: String,
    @Column(length = MAX_NUMBER_LENGTH)
    var senderNumber: String? = null,
    @Column(nullable = false)
    var title: String,
    @Column(length = SmsValidator.MAX_MESSAGE_LENGTH, nullable = false)
    var message: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var stat: UmsSendStat,
    /** 프로시저가 돌려준 원본 상태 코드. 정의되지 않은 값도 남긴다 */
    var statCode: Int? = null,
    var clidx: Long? = null,
    /** 드라이버 예외 메시지가 길어 기본 255자를 넘기므로 컬럼을 넓히고 저장 시 잘라 넣는다 */
    @Column(length = MAX_FAILURE_REASON_LENGTH)
    var failureReason: String? = null,
    @Enumerated(EnumType.STRING)
    var resultCode: UmsResultCode? = null,
    @Enumerated(EnumType.STRING)
    var statusCode: UmsStatusCode? = null,
    var errorCode: String? = null,
    var messageType: String? = null,
    var completedAt: LocalDateTime? = null,
    /** 결과를 못 받아도 갱신해, 확정되지 않는 건이 뒤쪽 건의 조회를 막지 않도록 순환시킨다 */
    var resultCheckedAt: LocalDateTime? = null,
) : BaseEntity() {
    val isResultConfirmed: Boolean
        get() = resultCode != null

    fun markResultChecked(row: UmsSendResultRow?) {
        resultCheckedAt = LocalDateTime.now()
        row ?: return
        resultCode = UmsResultCode.fromCode(row.resultCode)
        statusCode = UmsStatusCode.fromCode(row.statusCode)
        errorCode = row.errorCode
        messageType = row.messageType
        completedAt = row.completedAt
    }

    companion object {
        const val MAX_FAILURE_REASON_LENGTH = 1000

        /** 검증을 통과하지 못한 값도 이력에는 남으므로, 저장 전에 컬럼 길이로 자른다 */
        const val MAX_NUMBER_LENGTH = 40

        fun truncateFailureReason(reason: String?): String? = reason?.take(MAX_FAILURE_REASON_LENGTH)

        fun truncateNumber(number: String): String = number.take(MAX_NUMBER_LENGTH)
    }
}
