package com.pluxity.aiot.data.dto

/**
 * 산불 감지기(34958) FireCauseMask 비트 정의.
 *
 * 단말은 원인을 비트 마스크 정수 하나로 보고하며, 적재는 원값 그대로 하고
 * 조회 응답에서만 원인 목록으로 디코딩한다.
 */
enum class FireCause(
    val bit: Int,
    val description: String,
) {
    TEMPERATURE(0x01, "온도 이상"),
    CO(0x02, "CO 이상"),
    TVOC(0x04, "TVOC 이상"),
    HUMIDITY(0x08, "습도 이상"),
    CO2(0x10, "CO2 이상"),
    ;

    companion object {
        fun decode(mask: Int): List<FireCause> = entries.filter { mask and it.bit != 0 }
    }
}
