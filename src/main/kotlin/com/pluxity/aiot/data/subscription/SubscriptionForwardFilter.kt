package com.pluxity.aiot.data.subscription

import com.pluxity.aiot.global.config.RestClientFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import java.util.concurrent.Executors

private val log = KotlinLogging.logger {}

@ConfigurationProperties(prefix = "subscription.forward")
data class SubscriptionForwardProperties(
    val url: String = "",
)

/**
 * 임시 기능. 운영 서버는 내부망이라 Mobius 알림이 개발 서버까지 오지 않는다.
 * 받은 본문을 바이트 그대로 개발 서버에 넘긴다.
 * 운영 처리 성패와 무관하게 넘기고, 전달 실패는 로그만 남긴다.
 *
 * 주소가 비어 있지 않을 때만 빈이 만들어진다. ConditionalOnProperty는 빈 문자열도 존재로 보므로 쓰지 않는다.
 */
@Component
@ConditionalOnExpression("'\${subscription.forward.url:}' != ''")
class SubscriptionForwardFilter(
    private val properties: SubscriptionForwardProperties,
    private val restClientFactory: RestClientFactory,
) : OncePerRequestFilter() {
    private val client: RestClient by lazy { restClientFactory.createClient(properties.url) }
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || !request.requestURI.endsWith("/subscription")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val wrapped = ContentCachingRequestWrapper(request, CACHE_LIMIT_BYTES)
        try {
            filterChain.doFilter(wrapped, response)
        } finally {
            forward(wrapped.contentAsByteArray)
        }
    }

    private fun forward(body: ByteArray) {
        if (body.isEmpty()) return
        executor.execute {
            runCatching {
                client
                    .post()
                    .uri("/subscription")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity()
            }.onFailure { e -> log.warn(e) { "개발 서버로 알림 전달 실패: ${properties.url}" } }
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.close()
    }

    companion object {
        /** Mobius 알림은 1KB 안팎이다. 넘치면 잘린 본문이 가서 개발 서버가 400을 낸다 */
        private const val CACHE_LIMIT_BYTES = 1024 * 1024
    }
}
