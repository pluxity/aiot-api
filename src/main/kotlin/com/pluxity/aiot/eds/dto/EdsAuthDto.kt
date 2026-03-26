package com.pluxity.aiot.eds.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EdsLoginRequest(
    @field:JsonProperty("system_key")
    val systemKey: String,
    @field:JsonProperty("system_token")
    val systemToken: String,
)

data class EdsResponse<T>(
    val code: Int,
    val message: String,
    @field:JsonProperty("time_stamp")
    val timeStamp: String? = null,
    val result: T? = null,
)

data class EdsLoginResult(
    @field:JsonProperty("api-key")
    val apiKey: String,
)
