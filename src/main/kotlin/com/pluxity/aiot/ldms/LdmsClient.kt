package com.pluxity.aiot.ldms

import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.LdmsProperties
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import com.pluxity.aiot.ldms.dto.LdmsLoginRequest
import com.pluxity.aiot.ldms.dto.LdmsLoginResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.toEntity

private val log = KotlinLogging.logger {}

/**
 * LDMS는 로그인 응답의 id 세션 쿠키로 인증한다. 쿠키는 30분 뒤 만료되고,
 * 만료·누락 시 목록 API가 본문 없이 401을 주므로 그때 재로그인한다.
 */
@Component
@ConditionalOnProperty("ldms.enabled", havingValue = "true")
class LdmsClient(
    restClientFactory: RestClientFactory,
    private val ldmsProperties: LdmsProperties,
) {
    private val client: RestClient = restClientFactory.createClient(ldmsProperties.baseUrl)

    @Volatile
    private var sessionCookie: String? = null

    private fun login(): String {
        val cookie =
            try {
                val response =
                    client
                        .post()
                        .uri("/api/auth/login")
                        .body(LdmsLoginRequest(ldmsProperties.userId, ldmsProperties.password))
                        .retrieve()
                        .toEntity<LdmsLoginResult>()
                response.headers[HttpHeaders.SET_COOKIE]
                    .orEmpty()
                    .map { it.substringBefore(';') }
                    .firstOrNull { it.startsWith("$SESSION_COOKIE_NAME=") }
            } catch (e: RestClientException) {
                throw CustomException(ErrorCode.LDMS_LOGIN_FAILED, e.message ?: "알 수 없는 오류")
            } ?: throw CustomException(ErrorCode.LDMS_LOGIN_FAILED, "세션 쿠키 없음")

        sessionCookie = cookie
        log.info { "LDMS 로그인 성공" }
        return cookie
    }

    fun getEbrdList(): List<LdmsEbrdInfo> = getList("/api/ebrd/list", object : ParameterizedTypeReference<List<LdmsEbrdInfo>>() {})

    fun getEmcallGroupList(): List<LdmsEmcallGroupInfo> =
        getList("/api/emcall_grp/list", object : ParameterizedTypeReference<List<LdmsEmcallGroupInfo>>() {})

    private fun <T : Any> getList(
        path: String,
        type: ParameterizedTypeReference<List<T>>,
    ): List<T> =
        withSession {
            client
                .get()
                .uri(path)
                .header(HttpHeaders.COOKIE, it)
                .retrieve()
                .body(type)
                .orEmpty()
        }

    /**
     * 세션이 없으면 먼저 로그인하고, 401이면 재로그인 후 한 번만 다시 호출한다.
     * 연결 거부·타임아웃(ResourceAccessException)도 상위 타입으로 받아 502로 변환한다.
     */
    private fun <T> withSession(block: (cookie: String) -> T): T {
        val cookie = sessionCookie ?: login()
        return try {
            block(cookie)
        } catch (e: RestClientException) {
            if (e !is RestClientResponseException || e.statusCode != HttpStatus.UNAUTHORIZED) {
                throw CustomException(ErrorCode.LDMS_API_ERROR, e.message ?: "알 수 없는 오류")
            }
            log.warn { "LDMS API 401, 재로그인 후 재시도" }
            val renewed = login()
            try {
                block(renewed)
            } catch (retryError: RestClientException) {
                throw CustomException(ErrorCode.LDMS_API_ERROR, retryError.message ?: "알 수 없는 오류")
            }
        }
    }

    companion object {
        const val SESSION_COOKIE_NAME = "id"
    }
}
