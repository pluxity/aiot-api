package com.pluxity.aiot.global.logging

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
class TraceIdFilterConfig {
    /**
     * Spring Security의 FilterChainProxy보다 앞에 있어야 인증 실패 로그에도 traceId가 붙는다.
     * Logbook 필터는 LOWEST_PRECEDENCE라 양 끝값으로 순서가 보장된다.
     */
    @Bean
    fun traceIdFilterRegistration(): FilterRegistrationBean<TraceIdFilter> =
        FilterRegistrationBean(TraceIdFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
        }
}
