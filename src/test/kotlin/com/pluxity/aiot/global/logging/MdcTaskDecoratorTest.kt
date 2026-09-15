package com.pluxity.aiot.global.logging

import com.pluxity.aiot.global.config.AsyncConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.slf4j.MDC
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 데코레이터가 지키는 것은 두 가지다. 제출 스레드의 컨텍스트를 옮기는 것과 실행 후 원상 복구하는 것.
 * 후자가 빠지면 풀 스레드 재사용 시 다음 작업이 남의 traceId를 단다.
 */
class MdcTaskDecoratorTest :
    BehaviorSpec({
        val key = TraceIdFilter.KEY

        afterTest { MDC.clear() }

        Given("제출 스레드에 MDC가 있을 때") {
            When("다른 스레드에서 실행하면") {
                Then("제출 시점의 값이 보인다") {
                    MDC.put(key, "trace-A")
                    val seen = AtomicReference<String?>()
                    val latch = CountDownLatch(1)

                    val decorated =
                        MdcTaskDecorator.decorate {
                            seen.set(MDC.get(key))
                            latch.countDown()
                        }
                    MDC.put(key, "trace-B")
                    Thread(decorated).start()

                    latch.await(5, TimeUnit.SECONDS) shouldBe true
                    seen.get() shouldBe "trace-A"
                }
            }

            When("@Async용 taskExecutor로 제출하면") {
                Then("실행 스레드에서도 같은 traceId가 보인다") {
                    val executor = AsyncConfig().taskExecutor()
                    val seen = AtomicReference<String?>()
                    val latch = CountDownLatch(1)

                    MDC.put(key, "trace-async")
                    executor.execute {
                        seen.set(MDC.get(key))
                        latch.countDown()
                    }

                    latch.await(5, TimeUnit.SECONDS) shouldBe true
                    seen.get() shouldBe "trace-async"
                }
            }
        }

        Given("실행 스레드에 이전 작업의 MDC가 남아 있을 때") {
            When("작업이 끝나면") {
                Then("실행 전 상태로 원복된다") {
                    val decorated = MdcTaskDecorator.decorate { }

                    MDC.put(key, "leftover")
                    decorated.run()

                    MDC.get(key) shouldBe "leftover"
                }
            }

            When("작업이 예외를 던져도") {
                Then("원복된다") {
                    val decorated = MdcTaskDecorator.decorate { throw IllegalStateException("boom") }

                    MDC.put(key, "leftover")
                    shouldThrow<IllegalStateException> { decorated.run() }

                    MDC.get(key) shouldBe "leftover"
                }
            }
        }

        Given("제출 스레드에 MDC가 없을 때") {
            When("실행 스레드에 남은 값이 있으면") {
                Then("실행 중에는 비어 있고 끝나면 남은 값이 돌아온다") {
                    val decorated = MdcTaskDecorator.decorate { MDC.get(key).shouldBeNull() }

                    MDC.put(key, "leftover")
                    decorated.run()

                    MDC.get(key) shouldBe "leftover"
                }
            }
        }

        Given("withMdcEntry") {
            When("이미 같은 키에 값이 있는 상태에서 중첩 호출하면") {
                Then("블록 안에서는 새 값, 나오면 이전 값이다") {
                    MDC.put(DEVICE_ID_KEY, "outer")

                    withMdcEntry(DEVICE_ID_KEY, "inner") { MDC.get(DEVICE_ID_KEY) shouldBe "inner" }

                    MDC.get(DEVICE_ID_KEY) shouldBe "outer"
                }
            }
        }
    })
