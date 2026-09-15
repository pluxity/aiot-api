package com.pluxity.aiot.global.logging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import org.slf4j.MDC
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.atomic.AtomicReference

/**
 * AOP는 조용히 안 걸릴 수 있다. 프록시가 잡히지 않으면 컴파일도 다른 테스트도 통과하고
 * 그 경로 로그에서만 traceId가 사라진다. 실제 스프링 컨텍스트에서 @Scheduled 빈을 호출해 확인한다.
 */
class ScheduledTraceIdAspectTest :
    BehaviorSpec({
        val key = TraceIdFilter.KEY

        afterTest { MDC.clear() }

        Given("스프링 컨텍스트에 등록된 @Scheduled 빈") {
            AnnotationConfigApplicationContext(ProxyTestConfig::class.java).use { context ->
                val job = context.getBean<FakeScheduledJob>()

                When("스케줄러가 호출하듯 메서드를 부르면") {
                    Then("어드바이스가 걸려 traceId가 발급된다") {
                        job.run()

                        job.seen.get().shouldNotBeNull() shouldMatch Regex("[0-9a-f]{32}")
                    }
                }

                When("두 번 호출하면") {
                    Then("회차마다 다른 값이 발급된다") {
                        job.run()
                        val first = job.seen.get()
                        job.run()

                        job.seen.get() shouldNotBe first
                    }
                }

                When("작업이 끝나면") {
                    Then("MDC가 정리된다") {
                        job.run()

                        MDC.get(key).shouldBeNull()
                    }
                }

                When("작업이 예외를 던져도") {
                    Then("MDC가 정리된다") {
                        shouldThrow<IllegalStateException> { job.boom() }

                        MDC.get(key).shouldBeNull()
                    }
                }
            }
        }
    })

/**
 * 일반 @Configuration·@Component로 두면 @SpringBootTest의 컴포넌트 스캔에 잡혀
 * 무관한 통합 테스트에 어드바이스가 두 번 걸리고 boom()이 기동 직후 실행된다.
 */
@TestConfiguration
@EnableAspectJAutoProxy
private class ProxyTestConfig {
    @Bean fun aspect() = ScheduledTraceIdAspect()

    @Bean fun job() = FakeScheduledJob()
}

/** @Scheduled를 단 실제 빈. 어노테이션 포인트컷이 실제로 매칭되는지 확인한다. @Component가 없어 allopen이 안 열어주므로 직접 연다 */
open class FakeScheduledJob {
    open val seen = AtomicReference<String?>()

    @Scheduled(fixedDelay = Long.MAX_VALUE)
    open fun run() {
        seen.set(MDC.get(TraceIdFilter.KEY))
    }

    @Scheduled(fixedDelay = Long.MAX_VALUE)
    open fun boom(): Unit = throw IllegalStateException("boom")
}
