package com.pluxity.aiot.global.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.zalando.logbook.HttpRequest
import org.zalando.logbook.Logbook
import org.zalando.logbook.core.Conditions
import java.util.function.Predicate

private val log = KotlinLogging.logger {}

@Configuration
class LogbookConfig {
    @Bean
    fun logbook(): Logbook {
        // 1) 명시적 Predicate<HttpRequest>로 선언
        val excludePredicate: Predicate<HttpRequest> =
            Predicate { req ->
                // Logbook 3.x: req.uri.path  (Url 객체의 path)
                val path = req.path

                path.contains("/actuator/") ||
                    path.contains("/swagger-ui/") ||
                    path.contains("/api-docs/") ||
                    path.contains("/.well-known/") ||
                    path.contains("/springwolf/")
            }

        // 2) vararg 또는 collection 오버로드 중 하나 선택
        // vararg 예시:
        // val condition = Conditions.exclude<HttpRequest>(excludePredicate)

        // collection 오버로드 예시(타입 추론 더 잘 됨):
        val condition = Conditions.exclude(listOf(excludePredicate))

        return Logbook
            .builder()
            .condition(condition)
            .build()
    }
}
