package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/** enabled가 false면 실제 DB에 접속하지 않고 발송 요청을 로그로만 남긴다 */
@ConfigurationProperties(prefix = "ums")
data class UmsProperties(
    val enabled: Boolean = false,
    /** 예: jdbc:sqlserver://host:11433;databaseName=CIEL_UMS_HOME;encrypt=false */
    val url: String = "",
    val username: String = "",
    val password: String = "",
    /** @user_id */
    val systemAccount: String = "",
    /** @sub_code */
    val subCode: String = "",
    /** @send_number */
    val senderNumber: String = "",
    val connectionTimeoutMillis: Long = 5_000,
    val queryTimeoutSeconds: Int = 10,
    /** 이 시간이 지나도 확정되지 않은 건은 결과 조회를 포기한다 */
    val resultPollCutoffHours: Long = 24,
    val resultPollIntervalSeconds: Long = 60,
    val resultPollBatchSize: Int = 100,
)
