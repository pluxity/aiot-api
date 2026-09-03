package com.pluxity.aiot.mic

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.global.properties.MicProperties
import com.pluxity.aiot.mic.dto.MicEventData
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicWebSocketClient(
    private val micClient: MicClient,
    private val micProperties: MicProperties,
    private val objectMapper: ObjectMapper,
    private val micEventService: MicEventService,
) {
    @Volatile
    private var disposable: Disposable? = null

    @Volatile
    private var stopped = false

    fun connect() {
        disconnect()
        stopped = false

        val client = ReactorNettyWebSocketClient(HttpClient.create())

        disposable =
            // Mono.defer로 감싸야 재연결 시점에 갱신된 토큰을 다시 읽는다
            Mono
                .defer {
                    client.execute(buildUri(), buildHeaders()) { session ->
                        log.info { "AI 마이크 WebSocket 연결 성공" }

                        session
                            .receive()
                            .filter { it.type == WebSocketMessage.Type.TEXT }
                            .map { it.payloadAsText }
                            .publishOn(Schedulers.boundedElastic())
                            .doOnNext { handleMessage(it) }
                            .doOnError { e -> log.error(e) { "AI 마이크 WebSocket 오류" } }
                            .doOnComplete { log.info { "AI 마이크 WebSocket 연결 종료" } }
                            .then()
                    }
                }.doOnSuccess { if (!stopped) log.warn { "AI 마이크 WebSocket 연결 정상 종료, 재연결 예정" } }
                .repeatWhen { it.delayElements(Duration.ofSeconds(5)).takeWhile { !stopped } }
                .retryWhen(
                    Retry
                        .backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofMinutes(2))
                        .filter { !stopped }
                        .doBeforeRetry { log.info { "AI 마이크 WebSocket 재연결 시도 (${it.totalRetries() + 1}회)" } },
                ).subscribe()
    }

    fun disconnect() {
        stopped = true
        disposable?.dispose()
        disposable = null
    }

    private fun buildUri(): URI = URI(micProperties.baseUrl.replaceFirst(HTTP_SCHEME_REGEX, "ws") + WS_EVENTS_PATH)

    private fun buildHeaders(): HttpHeaders =
        HttpHeaders().apply {
            setBearerAuth(micClient.getAccessToken())
        }

    private fun handleMessage(json: String) {
        try {
            val event = objectMapper.readValue(json, MicEventData::class.java)
            log.info { "AI 마이크 이벤트: id=${event.id}, mic=${event.mic?.id}, label=${event.label?.id}" }
            micEventService.saveEvent(event)
        } catch (e: Exception) {
            log.error(e) { "AI 마이크 메시지 처리 오류: $json" }
        }
    }

    companion object {
        private val HTTP_SCHEME_REGEX = Regex("^http")
        private const val WS_EVENTS_PATH = "/ws/events"
    }
}
