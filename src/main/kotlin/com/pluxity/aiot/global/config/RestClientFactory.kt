package com.pluxity.aiot.global.config

import org.springframework.beans.factory.DisposableBean
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.function.Predicate

@Component
class RestClientFactory : DisposableBean {
    /** 연결 타임아웃만 HttpClient를 가른다. 호출마다 새로 만들면 셀렉터 스레드가 쌓인다. */
    private val clients = ConcurrentHashMap<Long, HttpClient>()

    /** 가상 스레드라 여러 개 둘 이득이 없다. 지정하지 않으면 JDK가 자체 플랫폼 풀을 만든다. */
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    fun createClient(
        baseUrl: String,
        connectionTimeoutMs: Long = 5000,
        readTimeoutMs: Long = 30000,
        /** 상태코드가 아니라 응답 본문으로 성패를 판정하는 연동처에 쓴다. */
        throwOnHttpError: Boolean = true,
    ): RestClient {
        val httpClient =
            clients.computeIfAbsent(connectionTimeoutMs) { timeout ->
                HttpClient
                    .newBuilder()
                    .executor(executor)
                    // 기본값 HTTP/2는 평문 연결에서 Upgrade: h2c를 붙인다.
                    // 본문 있는 요청에 이걸 붙이면 업그레이드를 못 다루는 서버가 본문을 흘린다
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofMillis(timeout))
                    .build()
            }

        val requestFactory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(Duration.ofMillis(readTimeoutMs))
            }

        val builder =
            RestClient
                .builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeaders { it.accept = listOf(MediaType.APPLICATION_JSON) }
                // Jackson 컨버터는 길이를 모른 채 흘려보내 Transfer-Encoding: chunked가 된다.
                // 인터셉터 단계에서는 본문이 이미 바이트로 잡혀 있어 길이를 붙일 수 있다
                .requestInterceptor { request, body, execution ->
                    request.headers.contentLength = body.size.toLong()
                    execution.execute(request, body)
                }

        if (!throwOnHttpError) {
            builder.defaultStatusHandler(Predicate<HttpStatusCode> { true }) { _, _ -> }
        }

        return builder.build()
    }

    override fun destroy() {
        clients.values.forEach { it.close() }
        clients.clear()
        executor.close()
    }
}
