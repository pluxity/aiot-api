package com.pluxity.aiot.eds

import com.pluxity.aiot.global.properties.EdsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsKeepAliveScheduler(
    private val edsClient: EdsClient,
    private val edsProperties: EdsProperties,
) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var task: ScheduledFuture<*>? = null

    fun start() {
        stop()
        val intervalSeconds = edsProperties.keepAliveTimeout / 2
        log.info { "EDS keepAlive 스케줄러 시작 (간격: ${intervalSeconds}초)" }
        task =
            scheduler.scheduleAtFixedRate(
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

    fun stop() {
        task?.cancel(false)
        task = null
    }
}
