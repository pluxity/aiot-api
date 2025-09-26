package com.pluxity.aiot.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "influx")
data class InfluxdbProperties
    @ConstructorBinding
    constructor(
        val url: String,
        val token: String,
        val org: String,
        val bucket: String,
    )
