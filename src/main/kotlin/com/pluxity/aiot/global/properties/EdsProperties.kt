package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "eds")
data class EdsProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val systemKey: String = "",
    val systemToken: String = "",
    val keepAliveTimeout: Long = 900,
    val reconnectDelay: Duration = Duration.ofSeconds(5),
    val reconnectMaxDelay: Duration = Duration.ofMinutes(2),
)
