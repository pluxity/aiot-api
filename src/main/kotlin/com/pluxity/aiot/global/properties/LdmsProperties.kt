package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ldms")
data class LdmsProperties(
    val enabled: Boolean = false,
    /** 호스트까지만 설정한다. 예: http://host:4011 */
    val baseUrl: String = "",
    val userId: String = "",
    val password: String = "",
)
