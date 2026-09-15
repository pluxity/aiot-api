package com.pluxity.aiot.global.utils

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateTimeUtils {
    private const val COMPACT_DATE_PATTERN = "yyyyMMdd"
    private const val COMPACT_DATETIME_PATTERN = "yyyyMMddHHmmss"

    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val formatterCompactDate = DateTimeFormatter.ofPattern(COMPACT_DATE_PATTERN)
    private val formatterCompactDateTime = DateTimeFormatter.ofPattern(COMPACT_DATETIME_PATTERN)

    fun parseUtc(input: String): Instant {
        val ldt = LocalDateTime.parse(input, formatter)
        val odt = ldt.atOffset(ZoneOffset.UTC)
        return odt.toInstant() // UTC Instant로 변환
    }

    fun formatToTimestamp(input: LocalDateTime): String = input.format(formatter)

    /** Mobius의 Timestamp·ct는 UTC다. 그대로 저장하면 이력이 9시간 이르게 보인다. */
    fun parseUtcToKst(timestamp: String): LocalDateTime =
        LocalDateTime
            .parse(timestamp, formatter)
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
            .toLocalDateTime()

    fun toIsoTimeFromKst(kstString: String): Instant {
        val localDateTime =
            LocalDateTime.parse(
                kstString,
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            )
        val zonedKST = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Seoul"))
        return zonedKST.toInstant()
    }

    fun parseCompactDate(dateString: String): LocalDate {
        try {
            return LocalDate.parse(dateString, formatterCompactDate)
        } catch (_: DateTimeParseException) {
            throw CustomException(ErrorCode.INVALID_DATE_TIME_FORMAT, COMPACT_DATE_PATTERN, dateString)
        }
    }

    fun parseCompactDateTime(dateTimeString: String): LocalDateTime {
        try {
            return LocalDateTime.parse(dateTimeString, formatterCompactDateTime)
        } catch (_: DateTimeParseException) {
            throw CustomException(ErrorCode.INVALID_DATE_TIME_FORMAT, COMPACT_DATETIME_PATTERN, dateTimeString)
        }
    }
}
