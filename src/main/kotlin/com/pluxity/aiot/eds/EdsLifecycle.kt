package com.pluxity.aiot.eds

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsLifecycle(
    private val edsClient: EdsClient,
    private val edsKeepAliveScheduler: EdsKeepAliveScheduler,
    private val edsService: EdsService,
    private val edsWebSocketClient: EdsWebSocketClient,
) : SmartLifecycle {
    private var running = false

    override fun start() {
        try {
            log.info { "EDS 연동 시작..." }
            edsClient.login()
            edsKeepAliveScheduler.start()
            edsService.sync()
            edsWebSocketClient.connect()
            running = true
            log.info { "EDS 연동 초기화 완료" }
        } catch (e: Exception) {
            log.error(e) { "EDS 연동 초기화 실패: ${e.message}" }
        }
    }

    override fun stop() {
        log.info { "EDS 연동 종료..." }
        edsWebSocketClient.disconnect()
        edsKeepAliveScheduler.stop()
        running = false
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = Int.MAX_VALUE - 1
}
