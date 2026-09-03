package com.pluxity.aiot.sms

import com.pluxity.aiot.global.entity.BaseEntity
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
    @Column(nullable = false)
    var targetNumber: String,
    var senderNumber: String? = null,
    @Column(nullable = false)
    var title: String,
    @Column(length = SmsValidator.MAX_MESSAGE_LENGTH, nullable = false)
    var message: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var stat: UmsSendStat,
    var clidx: Long? = null,
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
}
