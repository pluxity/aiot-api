package com.pluxity.aiot.announcement

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class LlmMessageScheduler(
    private val llmMessageService: LlmMessageService,
) {
    // 매일 오전 8시에 실행
    @Profile("!local")
    @Scheduled(cron = "0 0 8 * * ?")
    fun generateDailyMessage() {
        log.info { "일일 LLM 메시지 생성 스케줄러 시작" }
        try {
            runBlocking {
                llmMessageService.generateAndSaveMessage()
            }
            log.info { "일일 LLM 메시지 생성 완료" }
        } catch (e: Exception) {
            log.error(e) { "일일 LLM 메시지 생성 중 오류 발생" }
        }
    }
}
