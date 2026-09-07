package com.pluxity.aiot.global.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    // TaskScheduler 빈이 둘이므로 명시한다. 파라미터 이름 매칭은 @Primary에 밀린다
    @param:Qualifier("heartBeatScheduler") private val heartBeatScheduler: TaskScheduler,
    private val myDefaultHandshakeHandler: DefaultHandshakeHandler,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/stomp/platform") // 클라이언트가 연결할 엔드포인트
            .setAllowedOriginPatterns("*") // CORS 설정
            .setHandshakeHandler(myDefaultHandshakeHandler)
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry
            .enableSimpleBroker("/topic", "/queue") // 메시지 브로커 엔드포인트 prefix
            .setTaskScheduler(heartBeatScheduler)
            .setHeartbeatValue(longArrayOf(5000, 5000))
        registry.setApplicationDestinationPrefixes("/app") // 클라이언트에서 메시지 보낼 prefix
    }
}

@EnableAsync
@Configuration
class AsyncConfig {
    /**
     * 사용처가 0개여도 지우면 안 된다. 지운다고 자동설정이 살아나지 않고,
     * 나중에 붙는 @Async가 플랫폼·무제한 폴백으로 떨어진다.
     */
    @Bean(name = ["taskExecutor"])
    @Primary
    fun taskExecutor(): AsyncTaskExecutor =
        SimpleAsyncTaskExecutor("app-async-").apply {
            setVirtualThreads(true)
            setTaskTerminationTimeout(SHUTDOWN_WAIT_MILLIS)
        }

    @Bean
    fun heartBeatScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("stomp-heartbeat-")
            initialize()
        }

    /**
     * 이름이 정확히 taskScheduler여야 익명 단일 스레드 폴백 대신 이 빈이 쓰인다.
     * @Primary를 붙이면 이름 매칭을 이겨 하트비트 주입이 이쪽으로 넘어온다.
     * fixedDelay 작업은 스케줄러 스레드 하나에서 직접 도니 여럿 필요해지면 풀 기반으로 바꿔야 한다.
     */
    @Bean
    fun taskScheduler(): TaskScheduler =
        SimpleAsyncTaskScheduler().apply {
            setVirtualThreads(true)
            setThreadNamePrefix("app-sched-")
            // 넘겨진 작업은 종료 시 자동으로 기다려주지 않는다
            setTaskTerminationTimeout(SHUTDOWN_WAIT_MILLIS)
        }

    companion object {
        private const val SHUTDOWN_WAIT_MILLIS = 10_000L
    }
}
