package com.pluxity.aiot.global.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val formatterCompactDate = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val formatterCompactDateTime = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parseUtc(input: String): Instant {
        val ldt = LocalDateTime.parse(input, formatter)
        val odt = ldt.atOffset(ZoneOffset.UTC)
        return odt.toInstant() // UTC Instant로 변환
    }

    fun formatToTimestamp(input: LocalDateTime): String = input.format(formatter)

    fun safeParseFromTimestamp(timestamp: String): LocalDateTime = LocalDateTime.parse(timestamp, formatter)

    fun toIsoTimeFromKst(kstString: String): Instant {
        val localDateTime =
            LocalDateTime.parse(
                kstString,
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            )
        val zonedKST = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Seoul"))
        return zonedKST.toInstant()
    }

    fun parseCompactDate(dateString: String): LocalDate = LocalDate.parse(dateString, formatterCompactDate)

    fun parseCompactDateTime(dateTimeString: String): LocalDateTime = LocalDateTime.parse(dateTimeString, formatterCompactDateTime)
}
