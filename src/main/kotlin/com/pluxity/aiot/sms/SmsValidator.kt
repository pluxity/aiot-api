package com.pluxity.aiot.sms

/**
 * UMS 프로시저의 컬럼 제약을 호출 전에 검사한다.
 * 자릿수를 넘긴 번호는 프로시저가 stat 9로 거절하므로 미리 걸러 불필요한 호출을 줄인다.
 */
object SmsValidator {
    /** 문서 기준. 하이픈을 제외한 자릿수 상한 */
    const val MAX_NUMBER_DIGITS = 12

    /**
     * 하한은 문서에 없어 국내 번호 기준(지역번호 없는 시내번호 8자리)으로 둔다.
     * 형식만 보고 통과시키면 "abc"처럼 숫자가 하나도 없는 값이 그대로 넘어간다.
     */
    const val MIN_NUMBER_DIGITS = 8
    const val MAX_TITLE_LENGTH = 50
    const val MAX_MESSAGE_LENGTH = 2000

    /** 숫자로 시작하고 끝나며 사이에 하이픈만 허용한다 */
    private val NUMBER_FORMAT = Regex("^\\d[\\d-]*\\d$")

    /** 유효하면 null, 아니면 사유를 반환한다 */
    fun validate(
        request: SmsSendRequest,
        senderNumber: String,
    ): String? =
        when {
            senderNumber.isBlank() -> "발신번호가 설정되지 않았습니다"
            invalidNumberReason(senderNumber) != null -> "발신번호가 올바르지 않습니다: ${invalidNumberReason(senderNumber)}"
            request.targetNumber.isBlank() -> "수신번호가 비어 있습니다"
            invalidNumberReason(request.targetNumber) != null ->
                "수신번호가 올바르지 않습니다: ${invalidNumberReason(request.targetNumber)}"
            request.title.length > MAX_TITLE_LENGTH -> "제목이 ${MAX_TITLE_LENGTH}자를 초과합니다"
            request.message.isBlank() -> "내용이 비어 있습니다"
            request.message.length > MAX_MESSAGE_LENGTH -> "내용이 ${MAX_MESSAGE_LENGTH}자를 초과합니다"
            else -> null
        }

    /** 같은 번호의 표기 차이를 흡수하기 위한 정규화 값 */
    fun normalizeNumber(number: String): String = number.filter { it.isDigit() }

    private fun invalidNumberReason(number: String): String? {
        if (!NUMBER_FORMAT.matches(number)) return "숫자와 하이픈만 사용할 수 있습니다 ($number)"
        val digits = normalizeNumber(number).length
        return when {
            digits < MIN_NUMBER_DIGITS -> "자릿수가 ${MIN_NUMBER_DIGITS}자 미만입니다 ($number)"
            digits > MAX_NUMBER_DIGITS -> "자릿수가 ${MAX_NUMBER_DIGITS}자를 초과합니다 ($number)"
            else -> null
        }
    }
}
