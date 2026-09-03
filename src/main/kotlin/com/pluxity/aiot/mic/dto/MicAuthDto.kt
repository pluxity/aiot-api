package com.pluxity.aiot.mic.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class MicLoginRequest(
    val username: String,
    val password: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MicLoginResult(
    @field:JsonProperty("access_token")
    val accessToken: String,
    @field:JsonProperty("token_type")
    val tokenType: String? = null,
    @field:JsonProperty("expires_in")
    val expiresIn: Long? = null,
)
