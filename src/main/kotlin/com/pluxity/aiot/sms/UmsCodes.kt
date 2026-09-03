package com.pluxity.aiot.sms

/** sp_syncSend의 @return_stat */
enum class UmsSendStat(
    val code: Int,
    val description: String,
) {
    SUCCESS(0, "성공"),
    NO_ACCOUNT(-1, "계정없음"),
    NUMBER_TOO_LONG(9, "발신/수신번호 자릿수(12자) 초과"),

    /** 연동 전 또는 호출 자체가 실패해 프로시저 결과를 받지 못한 경우 */
    NOT_SENT(-999, "발송되지 않음"),
    ;

    companion object {
        fun fromCode(code: Int?): UmsSendStat = entries.find { it.code == code } ?: NOT_SENT
    }
}

/** view_sendResult의 RESULT */
enum class UmsResultCode(
    val code: Int,
) {
    SUCCESS(903),
    FAILURE(905),
    ;

    companion object {
        fun fromCode(code: Int?): UmsResultCode? = entries.find { it.code == code }
    }
}

/** view_sendResult의 STATUS */
enum class UmsStatusCode(
    val code: Int,
) {
    COMPLETED(333),
    CANCELED(334),
    ERROR(335),
    ;

    companion object {
        fun fromCode(code: Int?): UmsStatusCode? = entries.find { it.code == code }
    }
}
