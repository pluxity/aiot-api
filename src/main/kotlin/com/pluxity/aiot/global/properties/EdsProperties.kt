package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "eds")
data class EdsProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val systemKey: String = "",
    val systemToken: String = "",
    val keepAliveTimeout: Long = 900,
)
