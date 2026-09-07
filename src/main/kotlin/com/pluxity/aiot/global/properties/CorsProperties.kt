package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "cors")
data class CorsProperties
    @ConstructorBinding
    constructor(
        val allowedOriginPatterns: List<String>,
        val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
        val maxAge: Long = 3600L,
    )
