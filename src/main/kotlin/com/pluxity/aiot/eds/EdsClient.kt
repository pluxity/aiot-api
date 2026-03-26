package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsCameraInfo
import com.pluxity.aiot.eds.dto.EdsCameraListResponse
import com.pluxity.aiot.eds.dto.EdsLoginRequest
import com.pluxity.aiot.eds.dto.EdsLoginResult
import com.pluxity.aiot.eds.dto.EdsResponse
import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.EdsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsClient(
    webClientFactory: WebClientFactory,
    private val edsProperties: EdsProperties,
) {
    private val client: WebClient = webClientFactory.createClient(edsProperties.baseUrl)
    private lateinit var apiKey: String

    fun login() {
        val request = EdsLoginRequest(
            systemKey = edsProperties.systemKey,
            systemToken = edsProperties.systemToken,
        )

        val response = client
            .post()
            .uri("/api/eds/v1/external/users/login")
            .bodyValue(request)
            .exchangeToMono { resp ->
                resp.bodyToMono(object : ParameterizedTypeReference<EdsResponse<EdsLoginResult>>() {})
            }
            .block()
            ?: throw CustomException(ErrorCode.EDS_LOGIN_FAILED, "응답 없음")

        if (response.code != 200 || response.result == null) {
            throw CustomException(ErrorCode.EDS_LOGIN_FAILED, response.message)
        }

        apiKey = response.result.apiKey
        log.info { "EDS 로그인 성공" }
    }

    fun keepAlive() {
        try {
            val response = client
                .post()
                .uri("/api/eds/v1/external/users/keepalive")
                .header("api-key", apiKey)
                .exchangeToMono { resp ->
                    resp.bodyToMono(object : ParameterizedTypeReference<EdsResponse<Void>>() {})
                }
                .block()
                ?: throw CustomException(ErrorCode.EDS_API_ERROR, "keepAlive 응답 없음")

            if (response.code != 200) {
                throw CustomException(ErrorCode.EDS_API_ERROR, "keepAlive 실패: ${response.message}")
            }
            log.debug { "EDS keepAlive 성공" }
        } catch (e: Exception) {
            log.warn { "EDS keepAlive 실패, 재로그인 시도: ${e.message}" }
            login()
        }
    }

    fun getCameraList(): List<EdsCameraInfo> {
        val response = client
            .post()
            .uri("/api/eds/v1/external/camera/list")
            .header("api-key", apiKey)
            .exchangeToMono { resp ->
                resp.bodyToMono<EdsCameraListResponse>()
            }
            .block()
            ?: throw CustomException(ErrorCode.EDS_API_ERROR, "카메라 목록 응답 없음")

        if (response.code != 200) {
            throw CustomException(ErrorCode.EDS_API_ERROR, "카메라 목록 조회 실패: ${response.message}")
        }

        val cameras = response.result ?: emptyList()
        log.info { "EDS 카메라 목록 조회 완료: ${cameras.size}대 (전체: ${response.totalCount})" }
        return cameras
    }
}
