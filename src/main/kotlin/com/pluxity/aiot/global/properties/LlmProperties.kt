package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "llm.api")
data class LlmProperties(
    val baseUrl: String,
    val concurrencyLimit: Int = 5,
    /** 생성은 연동 API 호출보다 오래 걸린다. 짧게 잡으면 느린 사이트만 조용히 메시지가 빈다. */
    val responseTimeout: Duration = Duration.ofMinutes(3),
)
