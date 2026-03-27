package com.pluxity.aiot.eds

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.eds.dto.EdsCrowdCountData
import com.pluxity.aiot.eds.dto.EdsEventData
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers
import reactor.netty.http.client.HttpClient
import java.net.URI

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsWebSocketClient(
    private val edsClient: EdsClient,
    private val objectMapper: ObjectMapper,
    private val edsEventFacade: EdsEventFacade,
) {
    private var disposable: Disposable? = null

    fun connect() {
        disconnect()

        val wsBaseUrl = edsClient.getWebSocketUrl()
        val apiKey = edsClient.getApiKey()
        val uri = URI("$wsBaseUrl?api-key=$apiKey&evtMeta=begun,ended&crowdCountMeta")

        log.info { "EDS WebSocket 연결 시도" }

        val client = ReactorNettyWebSocketClient(HttpClient.create())

        disposable = client
            .execute(uri) { session ->
                log.info { "EDS WebSocket 연결 성공" }

                session
                    .receive()
                    .filter { it.type == WebSocketMessage.Type.TEXT }
                    .map { it.payloadAsText }
                    .publishOn(Schedulers.boundedElastic())
                    .doOnNext { handleMessage(it) }
                    .doOnError { e -> log.error(e) { "EDS WebSocket 오류" } }
                    .doOnComplete { log.info { "EDS WebSocket 연결 종료" } }
                    .then()
            }
            .doOnError { e -> log.error(e) { "EDS WebSocket 연결 실패" } }
            .subscribe()
    }

    fun disconnect() {
        disposable?.dispose()
        disposable = null
    }

    private fun handleMessage(json: String) {
        try {
            val tree = objectMapper.readTree(json)
            when {
                tree.has("event_start") -> {
                    val event = objectMapper.readValue(json, EdsEventData::class.java)
                    log.info { "EDS 이벤트: id=${event.id}, camera=${event.cameraId}, type=${EdsEventType.fromCode(event.type)?.description}, status=${EdsEventStatus.fromCode(event.status)?.description}" }
                    edsEventFacade.processEvent(event)
                }
                tree.has("zones") -> {
                    val crowdCount = objectMapper.readValue(json, EdsCrowdCountData::class.java)
                    log.info { "EDS 군중계수: camera=${crowdCount.cameraId}, total=${crowdCount.total}" }
                }
                else -> {
                    log.debug { "EDS 알 수 없는 메시지: $json" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "EDS 메시지 처리 오류" }
        }
    }
}
