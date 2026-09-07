package com.pluxity.aiot.eds

import com.pluxity.aiot.eds.dto.EdsCameraInfo
import com.pluxity.aiot.eds.dto.EdsLoginRequest
import com.pluxity.aiot.eds.dto.EdsLoginResult
import com.pluxity.aiot.eds.dto.EdsRealtimeStreamRequest
import com.pluxity.aiot.eds.dto.EdsRecordStreamRequest
import com.pluxity.aiot.eds.dto.EdsResponse
import com.pluxity.aiot.eds.dto.EdsStreamResult
import com.pluxity.aiot.eds.dto.EdsWebSocketUrlResult
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.EdsProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsClient(
    restClientFactory: RestClientFactory,
    private val edsProperties: EdsProperties,
) {
    /** EDS는 응답 본문의 code로 성패를 판정한다. 상태코드에 예외를 던지면 그 검사에 닿지 못한다. */
    private val client: RestClient = restClientFactory.createClient(edsProperties.baseUrl, throwOnHttpError = false)

    /** keepAlive 스케줄러 스레드가 쓰고 요청 스레드가 읽는다. */
    @Volatile
    private lateinit var apiKey: String

    fun getApiKey(): String = apiKey

    fun login() {
        val request =
            EdsLoginRequest(
                systemKey = edsProperties.systemKey,
                systemToken = edsProperties.systemToken,
            )

        val response =
            client
                .post()
                .uri("/api/eds/v1/external/users/login")
                .body(request)
                .retrieve()
                .body(object : ParameterizedTypeReference<EdsResponse<EdsLoginResult>>() {})
                ?: throw CustomException(ErrorCode.EDS_LOGIN_FAILED, "응답 없음")

        if (response.code != 200 || response.result == null) {
            throw CustomException(ErrorCode.EDS_LOGIN_FAILED, response.message)
        }

        apiKey = response.result.apiKey
        log.info { "EDS 로그인 성공" }
    }

    fun keepAlive() {
        try {
            val response =
                client
                    .post()
                    .uri("/api/eds/v1/external/users/keepalive")
                    .header("api-key", apiKey)
                    .retrieve()
                    .body(object : ParameterizedTypeReference<EdsResponse<Void>>() {})
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
        val response =
            client
                .post()
                .uri("/api/eds/v1/external/camera/list")
                .header("api-key", apiKey)
                .retrieve()
                .body(object : ParameterizedTypeReference<EdsResponse<List<EdsCameraInfo>>>() {})
                ?: throw CustomException(ErrorCode.EDS_API_ERROR, "카메라 목록 응답 없음")

        if (response.code != 200) {
            throw CustomException(ErrorCode.EDS_API_ERROR, "카메라 목록 조회 실패: ${response.message}")
        }

        val cameras = response.result ?: emptyList()
        log.info { "EDS 카메라 목록 조회 완료: ${cameras.size}대 (전체: ${response.totalCount})" }
        return cameras
    }

    fun getRealtimeStreamUrl(request: EdsRealtimeStreamRequest): EdsStreamResult {
        val response =
            client
                .post()
                .uri("/api/eds/v1/external/camera/stream/realtime")
                .header("api-key", apiKey)
                .body(request)
                .retrieve()
                .body(object : ParameterizedTypeReference<EdsResponse<EdsStreamResult>>() {})
                ?: throw CustomException(ErrorCode.EDS_API_ERROR, "실시간 스트림 URL 응답 없음")

        if (response.code != 200 || response.result == null) {
            throw CustomException(ErrorCode.EDS_API_ERROR, "실시간 스트림 URL 요청 실패: ${response.message}")
        }

        return response.result
    }

    fun getRecordStreamUrl(request: EdsRecordStreamRequest): EdsStreamResult {
        val response =
            client
                .post()
                .uri("/api/eds/v1/external/camera/stream/record")
                .header("api-key", apiKey)
                .body(request)
                .retrieve()
                .body(object : ParameterizedTypeReference<EdsResponse<EdsStreamResult>>() {})
                ?: throw CustomException(ErrorCode.EDS_API_ERROR, "녹화 스트림 URL 응답 없음")

        if (response.code != 200 || response.result == null) {
            throw CustomException(ErrorCode.EDS_API_ERROR, "녹화 스트림 URL 요청 실패: ${response.message}")
        }

        return response.result
    }

    /** Content-Length는 chunked면 -1이고 과소 신고도 가능해, 상한은 스트림에서 걸어야 한다. */
    fun getEventThumbnail(index: Long): ByteArray? =
        try {
            client
                .get()
                .uri("/api/eds/v1/external/event/thumbnail?index=$index&type=evtImg")
                .header("api-key", apiKey)
                .exchange { _, response ->
                    if (!response.statusCode.is2xxSuccessful) {
                        null
                    } else {
                        response.body.use { input ->
                            val bytes = input.readNBytes(MAX_THUMBNAIL_BYTES + 1)
                            if (bytes.size > MAX_THUMBNAIL_BYTES) {
                                log.warn { "EDS 썸네일이 상한을 넘어 버린다 (index=$index)" }
                                null
                            } else {
                                bytes
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            log.warn { "EDS 이벤트 썸네일 조회 실패 (index=$index): ${e.message}" }
            null
        }

    fun getWebSocketUrl(): String {
        val response =
            client
                .get()
                .uri("/api/eds/v1/external/websocket/url?external_flag=2")
                .header("api-key", apiKey)
                .retrieve()
                .body(object : ParameterizedTypeReference<EdsResponse<EdsWebSocketUrlResult>>() {})
                ?: throw CustomException(ErrorCode.EDS_API_ERROR, "웹소켓 URL 응답 없음")

        if (response.code != 200 || response.result == null) {
            throw CustomException(ErrorCode.EDS_API_ERROR, "웹소켓 URL 요청 실패: ${response.message}")
        }

        return response.result.wsUrl
    }

    companion object {
        /** readNBytes가 Int를 받는다 */
        private const val MAX_THUMBNAIL_BYTES = 1024 * 1024
    }
}
