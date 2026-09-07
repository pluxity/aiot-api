package com.pluxity.aiot.feature

import com.pluxity.aiot.sensor.type.AbbreviationData

/** 약어 테이블의 첫 단어를 이름으로 쓰고 끝의 숫자는 뒤에 붙인다. 약어가 없으면 원본 그대로다. */
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
