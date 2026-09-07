package com.pluxity.aiot.feature

import com.pluxity.aiot.sensor.type.AbbreviationData

/**
 * deviceId를 파싱하여 표시용 이름을 만든다.
 * 약어 테이블에 있는 첫 단어를 이름으로 쓰고, 끝의 숫자 식별자는 뒤에 붙인다.
 * 약어가 하나도 없으면 원본을 그대로 쓴다.
 */
fun parseDeviceName(
    deviceId: String,
    abbrMap: Map<String, AbbreviationData>,
): String {
    val parts =
        deviceId
            .replace("[\\s_]+".toRegex(), "-")
            .split("-")
            .filter { it.isNotBlank() }

    val name =
        parts
            .firstNotNullOfOrNull { abbrMap[it.lowercase()] }
            ?.fullName
            ?: return deviceId

    val numericSuffix =
        parts
            .lastOrNull()
            ?.takeIf { it.all(Char::isDigit) }
            .orEmpty()

    return buildString {
        append(name)
        if (numericSuffix.isNotEmpty()) {
            append("-")
            append(numericSuffix)
        }
    }
}
