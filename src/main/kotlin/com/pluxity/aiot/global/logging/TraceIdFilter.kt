package com.pluxity.aiot.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 요청마다 traceId를 발급해 MDC에 넣는다.
 *
 * 가상 스레드에서는 Tomcat 스레드명이 `tomcat-handler-N` 누적 일련번호라 스레드명으로
 * 한 요청의 로그를 묶을 수 없다. 체인 최앞단에서 돌아 Logbook·인증 실패 로그에도 traceId가 실린다.
 */
class TraceIdFilter : OncePerRequestFilter() {
    /** ASYNC 디스패치의 예외 핸들러 로그에도 traceId가 붙어야 한다 */
    override fun shouldNotFilterAsyncDispatch(): Boolean = false

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // 요청 헤더에서 읽지도, 응답 헤더로 내보내지도 않는다. 조작 가능한 값은 사고 조사 근거로 못 쓴다.
        // ASYNC 재디스패치는 처음 발급한 값을 되찾는다.
        val traceId =
            request.getAttribute(ATTRIBUTE) as? String
                ?: newTraceId().also { request.setAttribute(ATTRIBUTE, it) }
        MDC.put(KEY, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(KEY)
        }
    }

    companion object {
        const val KEY = "traceId"
        private const val ATTRIBUTE = "com.pluxity.aiot.traceId"
    }
}
