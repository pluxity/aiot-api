package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "llm.api")
data class LlmProperties(
    val baseUrl: String,
)
