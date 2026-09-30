package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ldms")
data class LdmsProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val userId: String = "",
    val password: String = "",
)
