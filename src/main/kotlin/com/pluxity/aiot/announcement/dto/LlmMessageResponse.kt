package com.pluxity.aiot.announcement.dto

import com.pluxity.aiot.announcement.LlmMessage
import java.time.LocalDateTime

data class LlmMessageResponse(
    val id: Long,
    val siteId: Long,
    val siteName: String,
    val yesterdayAvgTemp: Double,
    val todayAvgTemp: Double,
    val prompt: String,
    val message: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(llmMessage: LlmMessage): LlmMessageResponse =
            LlmMessageResponse(
                id = llmMessage.requiredId,
                siteId = llmMessage.site.requiredId,
                siteName = llmMessage.site.name,
                yesterdayAvgTemp = llmMessage.yesterdayAvgTemp,
                todayAvgTemp = llmMessage.todayAvgTemp,
                prompt = llmMessage.prompt,
                message = llmMessage.message,
                createdAt = llmMessage.createdAt,
                updatedAt = llmMessage.updatedAt,
            )
    }
}
