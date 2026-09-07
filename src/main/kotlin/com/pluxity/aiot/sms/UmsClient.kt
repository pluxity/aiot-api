package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsSendResult
import com.pluxity.aiot.sms.dto.UmsSendResultRow
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
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
    /**
     * Hikari가 던지는 메시지는 어느 설정이 비었는지 알려주지 않는다.
     * 발신번호·계정이 비면 기동은 되지만 모든 발송이 조용히 no-op이 되므로 함께 검사한다.
     */
    init {
        val missing =
            mapOf(
                "ums.url" to umsProperties.url,
                "ums.username" to umsProperties.username,
                "ums.system-account" to umsProperties.systemAccount,
                "ums.sub-code" to umsProperties.subCode,
                "ums.sender-number" to umsProperties.senderNumber,
            ).filterValues { it.isBlank() }.keys
        require(missing.isEmpty()) { "ums.enabled=true 이면 다음 설정이 필요합니다: ${missing.joinToString()}" }

        // 0을 넣으면 Hikari는 약 24.8일, JDBC 쿼리 타임아웃은 무제한이 되어 방어가 통째로 사라진다
        require(umsProperties.connectionTimeoutMillis >= MIN_CONNECTION_TIMEOUT_MILLIS) {
            "ums.connection-timeout-millis는 ${MIN_CONNECTION_TIMEOUT_MILLIS} 이상이어야 합니다"
        }
        require(umsProperties.queryTimeoutSeconds > 0) { "ums.query-timeout-seconds는 0보다 커야 합니다" }
        require(umsProperties.resultPollBatchSize > 0) { "ums.result-poll-batch-size는 0보다 커야 합니다" }
    }

    private val dataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = umsProperties.url
                username = umsProperties.username
                password = umsProperties.password
                maximumPoolSize = POOL_SIZE
                poolName = "ums-pool"
                // UMS가 죽어 있어도 애플리케이션 기동은 막지 않는다
                initializationFailTimeout = -1
                connectionTimeout = umsProperties.connectionTimeoutMillis
            },
        )

    // 타임아웃이 없으면 응답 없는 호스트에서 한 건이 스케줄러 스레드를 무한정 잡는다
    private val jdbcTemplate =
        JdbcTemplate(dataSource).apply {
            queryTimeout = umsProperties.queryTimeoutSeconds
        }

    /**
     * 문서 예시대로 7개 인자로 호출하고 반환 행(STAT, CLIDX)을 읽는다.
     * JDBC 이스케이프 구문을 쓰면 드라이버가 RPC로 호출해 문자열 파싱 차이를 타지 않는다.
     */
    fun syncSend(
        title: String,
        message: String,
        targetNumber: String,
        sendAt: LocalDateTime = LocalDateTime.now(),
    ): SmsSendResult {
        val row =
            jdbcTemplate
                .query(
                    SYNC_SEND_CALL,
                    { rs, _ -> rs.intOrNull("STAT") to rs.longOrNull("CLIDX") },
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
        val accepted = stat == UmsSendStat.SUCCESS.code
        return SmsSendResult(
            stat = UmsSendStat.fromCode(stat),
            statCode = stat,
            clidx = clidx?.takeIf { accepted },
            // CLIDX가 없으면 결과 조회 대상에서 빠지므로, 성공 응답이어도 추적 불가를 남긴다
            failureReason = "성공 응답에 CLIDX가 없어 결과를 추적할 수 없음".takeIf { accepted && clidx == null },
        )
    }

    fun findSendResult(clidx: Long): UmsSendResultRow? =
        jdbcTemplate
            .query(
                "SELECT RESULT, STATUS, ERRCODE, MSGGB, EDT FROM view_sendResult WHERE CLIDX = ?",
                { rs, _ ->
                    UmsSendResultRow(
                        resultCode = rs.intOrNull("RESULT"),
                        statusCode = rs.intOrNull("STATUS"),
                        errorCode = rs.getString("ERRCODE"),
                        messageType = rs.getString("MSGGB"),
                        completedAt = rs.getTimestamp("EDT")?.toLocalDateTime(),
                    )
                },
                clidx,
            ).firstOrNull()

    /** 컬럼이 smallint/numeric으로 오면 Int 캐스팅이 조용히 null이 되므로 Number로 받는다 */
    private fun ResultSet.intOrNull(column: String): Int? = (getObject(column) as? Number)?.toInt()

    /** CLIDX가 bigint면 Int로 좁힐 때 상위 비트가 조용히 잘려 결과 조회가 영영 매칭되지 않는다 */
    private fun ResultSet.longOrNull(column: String): Long? = (getObject(column) as? Number)?.toLong()

    override fun destroy() {
        log.info { "UMS 커넥션 풀 종료" }
        dataSource.close()
    }

    companion object {
        private const val POOL_SIZE = 2

        /** Hikari가 거부하는 하한 */
        private const val MIN_CONNECTION_TIMEOUT_MILLIS = 250L
        private const val SYNC_SEND_CALL = "{call sp_syncSend(?, ?, ?, ?, ?, ?, ?)}"
        private val SEND_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
