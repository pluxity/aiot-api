package com.pluxity.aiot.announcement.dto

data class LlmRequest(
    val prompt: String,
    val max_new_tokens: Int = 100,
    val temperature: Double = 0.7,
)
