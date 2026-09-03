package com.pluxity.aiot.mic

import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MicProperties
import com.pluxity.aiot.mic.dto.MicInfo
import com.pluxity.aiot.mic.dto.MicLoginRequest
import com.pluxity.aiot.mic.dto.MicLoginResult
import com.pluxity.aiot.mic.dto.MicPageResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger {}

@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicClient(
    webClientFactory: WebClientFactory,
    private val micProperties: MicProperties,
) {
    private val client: WebClient = webClientFactory.createClient(micProperties.baseUrl)

    @Volatile
    private var accessToken: String? = null

    @Volatile
    private var tokenTtlSeconds: Long = micProperties.tokenTtlFallback

    fun getAccessToken(): String = accessToken ?: throw CustomException(ErrorCode.MIC_LOGIN_FAILED, "토큰 없음")

    /** 토큰 갱신 주기 계산에 사용한다. 벤더 응답에 expires_in이 없으면 설정값으로 대체된다 */
    fun getTokenTtlSeconds(): Long = tokenTtlSeconds

    fun login() {
        val result =
            client
                .post()
                .uri("$API_PREFIX/auth/token")
                .bodyValue(MicLoginRequest(micProperties.username, micProperties.password))
                .retrieve()
                .bodyToMono<MicLoginResult>()
                .block()
                ?: throw CustomException(ErrorCode.MIC_LOGIN_FAILED, "응답 없음")

        accessToken = result.accessToken
        tokenTtlSeconds = result.expiresIn ?: micProperties.tokenTtlFallback
        log.info { "AI 마이크 로그인 성공 (토큰 유효시간: ${tokenTtlSeconds}초)" }
    }

    /** 벤더 목록 API가 페이징이라 전체 페이지를 순회해 모은다 */
    fun getMicList(): List<MicInfo> {
        val mics = mutableListOf<MicInfo>()
        var page = 1

        while (true) {
            val result = getMicPage(page, micProperties.listPageSize)
            mics += result.results
            if (page >= result.maxPage || result.results.isEmpty()) break
            page++
        }

        log.info { "AI 마이크 목록 조회 완료: ${mics.size}대" }
        return mics
    }

    private fun getMicPage(
        page: Int,
        size: Int,
    ): MicPageResult<MicInfo> =
        withRetryOnUnauthorized {
            client
                .get()
                .uri {
                    it
                        .path("$API_PREFIX/mics")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build()
                }.headers { headers -> headers.setBearerAuth(getAccessToken()) }
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<MicPageResult<MicInfo>>() {})
                .block()
                ?: throw CustomException(ErrorCode.MIC_API_ERROR, "마이크 목록 응답 없음")
        }

    /**
     * 토큰 갱신 스케줄러가 만료를 놓친 경우를 대비해, 401이면 재로그인 후 한 번만 다시 호출한다.
     * 재로그인과 재시도에서 난 오류도 도메인 예외로 변환해, 일반 500으로 새어 나가지 않게 한다.
     *
     * 연결 거부·DNS 실패·타임아웃은 WebClientRequestException이라 응답 예외의 하위 타입이 아니다.
     * 상위 타입으로 받지 않으면 업체 서버가 죽은 가장 흔한 상황에서만 502가 아닌 500이 나간다.
     */
    private fun <T> withRetryOnUnauthorized(block: () -> T): T =
        try {
            block()
        } catch (e: WebClientException) {
            if (e !is WebClientResponseException || e.statusCode != HttpStatus.UNAUTHORIZED) {
                throw CustomException(ErrorCode.MIC_API_ERROR, e.message)
            }
            log.warn { "AI 마이크 API 401, 재로그인 후 재시도" }
            retryAfterLogin(block)
        }

    private fun <T> retryAfterLogin(block: () -> T): T {
        try {
            login()
        } catch (e: Exception) {
            throw CustomException(ErrorCode.MIC_LOGIN_FAILED, e.message ?: "알 수 없는 오류")
        }

        return try {
            block()
        } catch (e: Exception) {
            throw CustomException(ErrorCode.MIC_API_ERROR, e.message ?: "알 수 없는 오류")
        }
    }

    companion object {
        /** 업체 API는 모든 엔드포인트가 이 접두사 아래에 있다. baseUrl에는 호스트만 설정한다 */
        const val API_PREFIX = "/api/v1"
    }
}
