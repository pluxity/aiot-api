package com.pluxity.aiot.mic

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * 벤더에 토큰 갱신 엔드포인트가 없어, 만료 전에 재로그인해 토큰을 새로 받는다.
 * 재로그인은 accessToken을 갱신하므로 단일 스레드로 직렬화한다.
 */
@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicTokenScheduler(
    private val micClient: MicClient,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    @Volatile
    private var task: ScheduledFuture<*>? = null

    fun start() {
        stop()
        val intervalSeconds = (micClient.getTokenTtlSeconds() / 2).coerceAtLeast(MIN_INTERVAL_SECONDS)
        log.info { "AI 마이크 토큰 갱신 스케줄러 시작 (간격: ${intervalSeconds}초)" }
        task =
            scheduler.scheduleAtFixedRate(
                {
                    try {
                        micClient.login()
                    } catch (e: Exception) {
                        log.error { "AI 마이크 토큰 갱신 실패: ${e.message}" }
                    }
                },
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS,
            )
    }

    fun stop() {
        task?.cancel(false)
        task = null
    }

    companion object {
        private const val MIN_INTERVAL_SECONDS = 60L
    }
}
