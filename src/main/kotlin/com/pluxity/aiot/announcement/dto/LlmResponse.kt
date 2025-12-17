package com.pluxity.aiot.announcement.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class LlmResponse(
    @field:JsonProperty("generated_text")
    val generatedText: String,
)
