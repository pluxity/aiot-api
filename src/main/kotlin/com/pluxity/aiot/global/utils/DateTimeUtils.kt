package com.pluxity.aiot.global.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun parseUtc(input: String): Instant {
        val ldt = LocalDateTime.parse(input, formatter)
        return ldt.toInstant(ZoneOffset.UTC) // UTC Instant로 변환
    }

    fun formatToTimestamp(input: LocalDateTime): String = input.format(formatter)

    fun safeParseFromTimestamp(timestamp: String): LocalDateTime = LocalDateTime.parse(timestamp, formatter)
}
