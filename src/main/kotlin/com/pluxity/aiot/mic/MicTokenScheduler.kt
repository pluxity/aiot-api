package com.pluxity.aiot.mic

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
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
    @Volatile
    private var scheduler: ScheduledExecutorService? = null

    fun start() {
        stop()
        val intervalSeconds = (micClient.getTokenTtlSeconds() / 2).coerceAtLeast(MIN_INTERVAL_SECONDS)
        log.info { "AI 마이크 토큰 갱신 스케줄러 시작 (간격: ${intervalSeconds}초)" }
        scheduler =
            newScheduler().apply {
                scheduleAtFixedRate(
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
    }

    /** 예약 작업뿐 아니라 실행기까지 정리한다. 재시작 시 [start]가 새로 만든다 */
    fun stop() {
        scheduler?.shutdownNow()
        scheduler = null
    }

    private fun newScheduler(): ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "mic-token-refresh").apply { isDaemon = true }
        }

    companion object {
        private const val MIN_INTERVAL_SECONDS = 60L
    }
}
