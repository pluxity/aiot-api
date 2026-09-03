package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mic")
data class MicProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    /** 로그인 응답에 expires_in이 없을 때 사용할 토큰 유효 시간(초) */
    val tokenTtlFallback: Long = 3600,
    /** 장비 목록 조회 페이지 크기 */
    val listPageSize: Int = 100,
)
