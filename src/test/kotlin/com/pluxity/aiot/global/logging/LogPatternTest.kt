package com.pluxity.aiot.global.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/**
 * 로그 패턴 렌더링 고정. 없는 필드는 자리도 남기지 않는다.
 * 패턴 문자열은 logback-spring.xml의 file appender와 같은 값을 유지해야 한다.
 */
class LogPatternTest :
    BehaviorSpec({
        val pattern =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} " +
                "%replace([tid:%X{traceId}] ){'\\[tid:\\] ',''}" +
                "%replace([dev:%X{deviceId}] ){'\\[dev:\\] ',''}" +
                "[%thread] %-5level %logger{36} - %msg%n"

        // 직접 만든 LoggerContext는 MDC 어댑터가 없어 LoggingEvent 생성이 NPE를 낸다
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        val layout =
            PatternLayout().apply {
                this.context = context
                this.pattern = pattern
                start()
            }

        fun render(): String =
            layout.doLayout(LoggingEvent("fqcn", context.getLogger("com.pluxity.aiot.Test"), Level.INFO, "메시지", null, null))

        afterTest { MDC.clear() }

        Given("traceId와 deviceId가 MDC에 있을 때") {
            When("로그를 찍으면") {
                Then("tid와 dev 접두어와 함께 나온다") {
                    MDC.put(TraceIdFilter.KEY, "b076dd0ccf464b828234b919de0be839")
                    MDC.put(DEVICE_ID_KEY, "SNIOT-P-WFL-001")

                    val line = render()

                    line shouldContain "[tid:b076dd0ccf464b828234b919de0be839]"
                    line shouldContain "[dev:SNIOT-P-WFL-001]"
                }
            }
        }

        Given("deviceId가 없을 때") {
            When("로그를 찍으면") {
                Then("dev 필드가 아예 나오지 않는다") {
                    MDC.put(TraceIdFilter.KEY, "b076dd0ccf464b828234b919de0be839")

                    val line = render()

                    line shouldNotContain "dev"
                    line shouldContain Regex("""\[tid:[0-9a-f]{32}] \[""")
                }
            }
        }

        Given("둘 다 없을 때") {
            When("기동 로그처럼 컨텍스트가 비어 있으면") {
                Then("tid와 dev 둘 다 생략된다") {
                    val line = render()

                    line shouldNotContain "tid"
                    line shouldNotContain "dev"
                    line shouldContain Regex("""\d{2}:\d{2}:\d{2}\.\d{3} \[""")
                }
            }
        }
    })
