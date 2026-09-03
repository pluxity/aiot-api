package com.pluxity.aiot.eds

import com.pluxity.aiot.global.properties.EdsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsKeepAliveScheduler(
    private val edsClient: EdsClient,
    private val edsProperties: EdsProperties,
) {
    @Volatile
    private var scheduler: ScheduledExecutorService? = null

    fun start() {
        stop()
        val intervalSeconds = edsProperties.keepAliveTimeout / 2
        log.info { "EDS keepAlive 스케줄러 시작 (간격: ${intervalSeconds}초)" }
        scheduler =
            newScheduler().apply {
                scheduleAtFixedRate(
                    {
                        try {
                            edsClient.keepAlive()
                        } catch (e: Exception) {
                            log.error { "EDS keepAlive 스케줄러 실행 중 오류: ${e.message}" }
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
            Thread(runnable, "eds-keepalive").apply { isDaemon = true }
        }
}
