package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "influx")
data class InfluxdbProperties(
    val url: String,
    val token: String,
    val org: String,
    val bucket: String,
)
