package com.pluxity.aiot.sms

import com.pluxity.aiot.sms.dto.SmsSendRequest

/** UMS 프로시저의 컬럼 제약을 호출 전에 검사한다 */
object SmsValidator {
    const val MAX_NUMBER_DIGITS = 12

    /** 하한은 문서에 없어 국내 번호 기준(시내번호 8자리)으로 둔다 */
    const val MIN_NUMBER_DIGITS = 8
    const val MAX_TITLE_LENGTH = 50
    const val MAX_MESSAGE_LENGTH = 2000

    private val NUMBER_FORMAT = Regex("^\\d[\\d-]*\\d$")

    private const val MASK_HEAD = 3
    private const val MASK_TAIL = 2

    fun validate(
        request: SmsSendRequest,
        senderNumber: String,
    ): String? {
        if (senderNumber.isBlank()) return "발신번호가 설정되지 않았습니다"
        numberErrorOrNull(senderNumber)?.let { return "발신번호가 올바르지 않습니다: $it" }
        if (request.targetNumber.isBlank()) return "수신번호가 비어 있습니다"
        numberErrorOrNull(request.targetNumber)?.let { return "수신번호가 올바르지 않습니다: $it" }
        return validateContent(request.title, request.message)
    }

    fun validateContent(
        title: String,
        message: String,
    ): String? =
        when {
            title.isBlank() -> "제목이 비어 있습니다"
            title.length > MAX_TITLE_LENGTH -> "제목이 ${MAX_TITLE_LENGTH}자를 초과합니다"
            message.isBlank() -> "내용이 비어 있습니다"
            message.length > MAX_MESSAGE_LENGTH -> "내용이 ${MAX_MESSAGE_LENGTH}자를 초과합니다"
            else -> null
        }

    fun normalizeNumber(number: String): String = number.filter { it.isDigit() }

    /** 앞 3자리와 뒤 2자리만 남긴다. 8자리 번호도 가려지는 자리가 남도록 뒤를 4자리에서 줄였다 */
    fun maskNumber(number: String): String {
        val digits = normalizeNumber(number)
        if (digits.length < MIN_NUMBER_DIGITS) return "***"
        return digits.take(MASK_HEAD) + "*".repeat(digits.length - MASK_HEAD - MASK_TAIL) + digits.takeLast(MASK_TAIL)
    }

    fun numberErrorOrNull(number: String): String? {
        if (!NUMBER_FORMAT.matches(number)) return "숫자와 하이픈만 사용할 수 있습니다"
        val digits = normalizeNumber(number).length
        return when {
            digits < MIN_NUMBER_DIGITS -> "자릿수가 ${MIN_NUMBER_DIGITS}자 미만입니다"
            digits > MAX_NUMBER_DIGITS -> "자릿수가 ${MAX_NUMBER_DIGITS}자를 초과합니다"
            else -> null
        }
    }
}
