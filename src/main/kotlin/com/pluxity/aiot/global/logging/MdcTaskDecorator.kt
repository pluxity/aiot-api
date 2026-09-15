package com.pluxity.aiot.global.logging

import org.slf4j.MDC
import org.springframework.core.task.TaskDecorator
import java.util.UUID

/**
 * 제출 스레드의 MDC를 실행 스레드로 옮기는 데코레이터.
 *
 * MDC는 ThreadLocal이라 스레드가 바뀌면 끊긴다. 가상 스레드라도 마찬가지다
 * (logback의 LogbackMDCAdapter는 InheritableThreadLocal이 아니다).
 */
object MdcTaskDecorator : TaskDecorator {
    override fun decorate(runnable: Runnable): Runnable {
        // decorate()는 제출 스레드에서 호출된다. 여기서 캡처해야 요청 컨텍스트가 잡힌다
        val captured = MDC.getCopyOfContextMap()
        return Runnable { withMdc(captured) { runnable.run() } }
    }
}

/** 구독 알림 처리 상관관계 키. 로그 패턴에서 `[dev:...]`로 렌더링된다. */
const val DEVICE_ID_KEY = "deviceId"

/** W3C trace-id와 같은 형식(32 hex 소문자)이라 Micrometer Tracing으로 옮겨도 로그 패턴을 안 바꿔도 된다. */
fun newTraceId(): String = UUID.randomUUID().toString().replace("-", "")

/** MDC에 [key]=[value]를 잠시 넣고 [block]을 실행한 뒤 이전 값으로 되돌린다. 중첩 호출에서도 안전하다. */
inline fun <T> withMdcEntry(
    key: String,
    value: String,
    block: () -> T,
): T {
    val previous = MDC.get(key)
    MDC.put(key, value)
    return try {
        block()
    } finally {
        previous?.let { MDC.put(key, it) } ?: MDC.remove(key)
    }
}

/**
 * [captured]를 현재 스레드에 잠시 설치하고 [block]을 실행한 뒤 원래대로 되돌린다.
 * 캡처는 반드시 MDC가 살아 있는 스레드에서 미리 해 두어야 한다.
 */
inline fun <T> withMdc(
    captured: Map<String, String>?,
    block: () -> T,
): T {
    val previous = MDC.getCopyOfContextMap()
    captured?.let(MDC::setContextMap) ?: MDC.clear()
    return try {
        block()
    } finally {
        // 풀 스레드는 재사용되므로 반드시 되돌린다. 틀린 traceId는 없는 traceId보다 나쁘다
        previous?.let(MDC::setContextMap) ?: MDC.clear()
    }
}
