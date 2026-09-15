package com.pluxity.aiot.global.logging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * 등록 순서 테스트가 특히 중요하다. order가 밀리면 인증 실패·예외 로그와 Logbook 줄에서
 * traceId가 조용히 사라진다. 컴파일도 다른 테스트도 통과하므로 사람이 알아채기 어렵다.
 */
class TraceIdFilterTest :
    BehaviorSpec({
        val key = TraceIdFilter.KEY

        afterTest { MDC.clear() }

        fun capturingChain(sink: (String?) -> Unit): FilterChain = { _, _ -> sink(MDC.get(key)) }

        Given("TraceIdFilter") {
            When("요청이 들어오면") {
                Then("체인 실행 중 32 hex 소문자 traceId가 MDC에 있다") {
                    var seen: String? = null
                    TraceIdFilter().doFilter(
                        MockHttpServletRequest("GET", "/features"),
                        MockHttpServletResponse(),
                        capturingChain { seen = it },
                    )

                    seen.shouldNotBeNull() shouldMatch Regex("[0-9a-f]{32}")
                }
            }

            When("요청이 끝나면") {
                Then("MDC가 정리된다") {
                    TraceIdFilter().doFilter(MockHttpServletRequest("GET", "/features"), MockHttpServletResponse(), MockFilterChain())

                    MDC.get(key).shouldBeNull()
                }
            }

            When("체인이 예외를 던져도") {
                Then("MDC가 정리된다") {
                    val boom = RuntimeException("boom")

                    shouldThrow<RuntimeException> {
                        TraceIdFilter().doFilter(
                            MockHttpServletRequest("GET", "/features"),
                            MockHttpServletResponse(),
                        ) { _, _ -> throw boom }
                    } shouldBe boom

                    MDC.get(key).shouldBeNull()
                }
            }

            When("요청이 두 번 들어오면") {
                Then("매번 다른 traceId를 발급한다") {
                    val ids = mutableListOf<String?>()
                    repeat(2) {
                        TraceIdFilter().doFilter(
                            MockHttpServletRequest("GET", "/features"),
                            MockHttpServletResponse(),
                            capturingChain {
                                ids +=
                                    it
                            },
                        )
                    }

                    ids[0] shouldNotBe ids[1]
                }
            }
        }

        Given("같은 요청이 ASYNC 디스패치로 다시 들어오면") {
            When("필터가 다시 처리하면") {
                Then("처음 발급한 traceId를 그대로 쓴다") {
                    val request = MockHttpServletRequest("GET", "/features")
                    var first: String? = null
                    var second: String? = null

                    TraceIdFilter().doFilter(request, MockHttpServletResponse(), capturingChain { first = it })
                    request.dispatcherType = DispatcherType.ASYNC
                    TraceIdFilter().doFilter(request, MockHttpServletResponse(), capturingChain { second = it })

                    first.shouldNotBeNull()
                    second shouldBe first
                }
            }
        }

        Given("요청 헤더에 X-Trace-Id가 실려 와도") {
            When("필터가 처리하면") {
                Then("무시하고 새로 발급하며 응답 헤더에도 싣지 않는다") {
                    val request = MockHttpServletRequest("GET", "/features").apply { addHeader("X-Trace-Id", "client-supplied-value") }
                    val response = MockHttpServletResponse()
                    var seen: String? = null

                    TraceIdFilter().doFilter(request, response, capturingChain { seen = it })

                    seen shouldNotBe "client-supplied-value"
                    seen.shouldNotBeNull() shouldMatch Regex("[0-9a-f]{32}")
                    response.getHeader("X-Trace-Id").shouldBeNull()
                }
            }
        }

        Given("필터 등록") {
            When("order를 확인하면") {
                Then("HIGHEST_PRECEDENCE다") {
                    TraceIdFilterConfig().traceIdFilterRegistration().order shouldBe Ordered.HIGHEST_PRECEDENCE
                }
            }
        }
    })
