package com.pluxity.aiot.global.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

/**
 * `@Scheduled` 실행마다 traceId를 새로 발급한다. 스케줄 작업은 상류가 없어 이어 붙일 값이 없다.
 * 스케줄러에 TaskDecorator를 거는 방식은 발사 시점의 트리거 스레드에서 빈 컨텍스트를 캡처해 조용히 실패한다.
 */
@Aspect
@Component
class ScheduledTraceIdAspect {
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    fun aroundScheduled(joinPoint: ProceedingJoinPoint): Any? = withMdcEntry(TraceIdFilter.KEY, newTraceId()) { joinPoint.proceed() }
}
