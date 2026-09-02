package com.pluxity.aiot.mic

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicLifecycle(
    private val micClient: MicClient,
    private val micTokenScheduler: MicTokenScheduler,
    private val micFacade: MicFacade,
    private val micWebSocketClient: MicWebSocketClient,
) : SmartLifecycle {
    private var running = false

    override fun start() {
        try {
            log.info { "AI 마이크 연동 시작..." }
            micClient.login()
            micTokenScheduler.start()
            micFacade.sync()
            micWebSocketClient.connect()
            running = true
            log.info { "AI 마이크 연동 초기화 완료" }
        } catch (e: Exception) {
            log.error(e) { "AI 마이크 연동 초기화 실패: ${e.message}" }
        }
    }

    override fun stop() {
        log.info { "AI 마이크 연동 종료..." }
        micWebSocketClient.disconnect()
        micTokenScheduler.stop()
        running = false
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MAX_VALUE - 1
}
