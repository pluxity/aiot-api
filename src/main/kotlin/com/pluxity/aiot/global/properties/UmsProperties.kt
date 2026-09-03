package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * UMS 문자시스템 연계 설정.
 * enabled가 false면 실제 DB에 접속하지 않고 발송 요청을 로그로만 남긴다.
 */
@ConfigurationProperties(prefix = "ums")
data class UmsProperties(
    val enabled: Boolean = false,
    /** MSSQL JDBC URL. 예: jdbc:sqlserver://host:11433;databaseName=CIEL_UMS_HOME;encrypt=false */
    val url: String = "",
    val username: String = "",
    val password: String = "",
    /** 연동 시스템별로 발급받은 시스템계정 (@user_id) */
    val systemAccount: String = "",
    /** 계정별 구분용 임의 코드 (@sub_code) */
    val subCode: String = "",
    /** 발신번호 (@send_number). 하이픈 제외 12자 이내 */
    val senderNumber: String = "",
    /** 발송 결과 폴링 주기(초) */
    val resultPollIntervalSeconds: Long = 60,
    /** 한 번에 결과를 조회할 최대 건수 */
    val resultPollBatchSize: Int = 100,
)
