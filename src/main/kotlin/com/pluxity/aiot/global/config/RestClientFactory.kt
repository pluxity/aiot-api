package com.pluxity.aiot.global.config

import org.springframework.beans.factory.DisposableBean
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.function.Predicate

@Component
class RestClientFactory : DisposableBean {
    /** HttpClient는 셀렉터 스레드와 커넥션 풀을 들고 있어 컨텍스트 종료 때 닫아야 한다. */
    private val clients = CopyOnWriteArrayList<HttpClient>()

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
            HttpClient
                .newBuilder()
                .executor(executor)
                .connectTimeout(Duration.ofMillis(connectionTimeoutMs))
                .build()
        clients += httpClient

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

        if (!throwOnHttpError) {
            builder.defaultStatusHandler(Predicate<HttpStatusCode> { true }) { _, _ -> }
        }

        return builder.build()
    }

    override fun destroy() {
        clients.forEach { it.close() }
        clients.clear()
        executor.close()
    }
}
