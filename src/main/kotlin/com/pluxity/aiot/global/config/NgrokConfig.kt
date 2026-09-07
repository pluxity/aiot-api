package com.pluxity.aiot.global.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.io.BufferedReader
import java.io.InputStreamReader

private val log = KotlinLogging.logger {}

@Component
@Profile("local")
class NgrokConfig(
    restClientFactory: RestClientFactory,
) {
    private var ngrokUrl: String = "http://localhost:8080" // 기본값으로 초기화

    // getter 메서드 추가
    fun getNgrokUrl(): String = ngrokUrl

    // 로컬 편의 기능이라 ngrok이 안 떠 있으면 빨리 포기해야 한다
    private val client: RestClient =
        restClientFactory.createClient(
            baseUrl = "http://localhost:4040",
            connectionTimeoutMs = 2000,
            readTimeoutMs = 5000,
        )

    @PostConstruct
    fun initNgrokUrl() {
        if (!this.isNgrokRunning()) {
            startNgrok()
            // ngrok 시작 후 잠시 대기 (API가 준비될 때까지)
            try {
                Thread.sleep(3000)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        fetchNgrokUrl()
    }

    private fun isNgrokRunning(): Boolean =
        try {
            client
                .get()
                .uri("/api/tunnels")
                .retrieve()
                .toBodilessEntity()
                .statusCode
                .is2xxSuccessful
        } catch (_: Exception) {
            false
        }

    private fun startNgrok() {
        try {
            log.info { "Starting ngrok..." }
            val processBuilder = ProcessBuilder("ngrok", "http", "8080", "--url=http://")
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()

            // 비동기로 ngrok 로그 읽기
            Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        log.debug { "ngrok: $line" }
                    }
                }
            }.start()

            log.info { "ngrok started successfully" }
        } catch (e: Exception) {
            log.error { "Failed to start ngrok: ${e.message}" }
            throw RuntimeException("Failed to start ngrok", e)
        }
    }

    private fun fetchNgrokUrl() {
        try {
            val jsonResponse =
                client
                    .get()
                    .uri("/api/tunnels")
                    .retrieve()
                    .body(String::class.java) ?: throw RuntimeException("Empty response from Ngrok API")

            if (!jsonResponse.contains("\"public_url\":\"")) {
                throw RuntimeException("Invalid response from Ngrok API")
            }

            val startIndex = jsonResponse.indexOf("\"public_url\":\"") + "\"public_url\":\"".length
            val endIndex = jsonResponse.indexOf("\"", startIndex)
            ngrokUrl = jsonResponse.substring(startIndex, endIndex)

            if (ngrokUrl.startsWith("https://")) {
                log.error { "$ngrokUrl is startsWith https" }
                ngrokUrl = "http://" + ngrokUrl.substring(8)
            }

            log.info { "Ngrok URL initialized: $ngrokUrl" }
        } catch (e: Exception) {
            log.error { "Failed to get Ngrok URL: $e.message" }
        }
    }
}
