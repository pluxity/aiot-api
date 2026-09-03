package com.pluxity.aiot.sms

/** UMS 프로시저의 컬럼 제약을 호출 전에 검사한다 */
object SmsValidator {
    const val MAX_NUMBER_DIGITS = 12

    /** 하한은 문서에 없어 국내 번호 기준(시내번호 8자리)으로 둔다 */
    const val MIN_NUMBER_DIGITS = 8
    const val MAX_TITLE_LENGTH = 50
    const val MAX_MESSAGE_LENGTH = 2000

    private val NUMBER_FORMAT = Regex("^\\d[\\d-]*\\d$")

    fun validate(
        request: SmsSendRequest,
        senderNumber: String,
    ): String? =
        when {
            senderNumber.isBlank() -> "발신번호가 설정되지 않았습니다"
            numberErrorOrNull(senderNumber) != null -> "발신번호가 올바르지 않습니다: ${numberErrorOrNull(senderNumber)}"
            request.targetNumber.isBlank() -> "수신번호가 비어 있습니다"
            numberErrorOrNull(request.targetNumber) != null ->
                "수신번호가 올바르지 않습니다: ${numberErrorOrNull(request.targetNumber)}"
            else -> validateContent(request.title, request.message)
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

    fun isValidNumber(number: String): Boolean = number.isNotBlank() && numberErrorOrNull(number) == null

    fun numberErrorOrNull(number: String): String? {
        if (!NUMBER_FORMAT.matches(number)) return "숫자와 하이픈만 사용할 수 있습니다 ($number)"
        val digits = normalizeNumber(number).length
        return when {
            digits < MIN_NUMBER_DIGITS -> "자릿수가 ${MIN_NUMBER_DIGITS}자 미만입니다 ($number)"
            digits > MAX_NUMBER_DIGITS -> "자릿수가 ${MAX_NUMBER_DIGITS}자를 초과합니다 ($number)"
            else -> null
        }
    }
}
