package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aiot.subscription")
data class SubscriptionProperties(
    val url: String = "",
)
