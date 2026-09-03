package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * DataSource 타입 빈을 노출하면 Spring Boot의 기본 DataSource 오토컨피그가 물러나 기존 JPA 설정이
 * 깨지므로, 커넥션 풀을 이 컴포넌트가 직접 들고 JdbcTemplate만 내부에서 사용한다.
 */
@Component
@ConditionalOnProperty("ums.enabled", havingValue = "true")
class UmsClient(
    private val umsProperties: UmsProperties,
) : DisposableBean {
    private val dataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = umsProperties.url
                username = umsProperties.username
                password = umsProperties.password
                maximumPoolSize = POOL_SIZE
                poolName = "ums-pool"
            },
        )

    private val jdbcTemplate = JdbcTemplate(dataSource)

    /** 문서 예시대로 7개 인자로 호출하고 반환 행(STAT, CLIDX)을 읽는다 */
    fun syncSend(
        title: String,
        message: String,
        targetNumber: String,
        sendAt: LocalDateTime = LocalDateTime.now(),
    ): SmsSendResult {
        val row =
            jdbcTemplate
                .query(
                    "EXEC sp_syncSend ?, ?, ?, ?, ?, ?, ?",
                    { rs, _ -> rs.getInt("STAT") to rs.getInt("CLIDX") },
                    umsProperties.systemAccount,
                    umsProperties.subCode,
                    title,
                    sendAt.format(SEND_DATETIME_FORMAT),
                    umsProperties.senderNumber,
                    message,
                    targetNumber,
                ).firstOrNull()
                ?: return SmsSendResult(UmsSendStat.NOT_SENT, failureReason = "sp_syncSend 응답 없음")

        val (stat, clidx) = row
        return SmsSendResult(
            stat = UmsSendStat.fromCode(stat),
            clidx = clidx.toLong().takeIf { stat == UmsSendStat.SUCCESS.code },
        )
    }

    fun findSendResult(clidx: Long): UmsSendResultRow? =
        jdbcTemplate
            .query(
                "SELECT RESULT, STATUS, ERRCODE, MSGGB, EDT FROM view_sendResult WHERE CLIDX = ?",
                { rs, _ ->
                    UmsSendResultRow(
                        resultCode = rs.getObject("RESULT") as? Int,
                        statusCode = rs.getObject("STATUS") as? Int,
                        errorCode = rs.getString("ERRCODE"),
                        messageType = rs.getString("MSGGB"),
                        completedAt = rs.getTimestamp("EDT")?.toLocalDateTime(),
                    )
                },
                clidx,
            ).firstOrNull()

    override fun destroy() {
        log.info { "UMS 커넥션 풀 종료" }
        dataSource.close()
    }

    companion object {
        private const val POOL_SIZE = 2
        private val SEND_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}

data class UmsSendResultRow(
    val resultCode: Int?,
    val statusCode: Int?,
    val errorCode: String?,
    val messageType: String?,
    val completedAt: LocalDateTime?,
)
