package com.pluxity.aiot.global.lifecycle

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * 지정한 횟수만큼 실패한 뒤 성공하는 초기화를 흉내낸다.
 */
private class FakeLifecycle(
    private val failCount: Int,
    private val onInitialize: () -> Unit = {},
) : RetryingLifecycle("테스트", initialRetryDelaySeconds = 1, maxRetryDelaySeconds = 2) {
    val initializeCalls = AtomicInteger()
    val shutdownCalls = AtomicInteger()

    override fun initialize() {
        val call = initializeCalls.getAndIncrement()
        onInitialize()
        if (call < failCount) {
            throw IllegalStateException("초기화 실패")
        }
    }

    override fun shutdown() {
        shutdownCalls.incrementAndGet()
    }
}

class RetryingLifecycleKoTest :
    BehaviorSpec({

        Given("외부 연동 기동") {
            When("초기화가 처음부터 성공") {
                val lifecycle = FakeLifecycle(failCount = 0)
                lifecycle.start()

                Then("재시도 없이 초기화가 완료된다") {
                    lifecycle.isInitialized shouldBe true
                    lifecycle.isRunning shouldBe true
                    lifecycle.initializeCalls.get() shouldBe 1
                }

                lifecycle.stop()
            }

            When("초기화가 두 번 실패한 뒤 성공") {
                val lifecycle = FakeLifecycle(failCount = 2)
                lifecycle.start()

                Then("초기화 전에도 isRunning은 true라 Spring이 종료 시 stop을 호출할 수 있다") {
                    lifecycle.isRunning shouldBe true
                    lifecycle.isInitialized shouldBe false
                }

                Then("재시도가 이어져 결국 초기화가 완료된다") {
                    eventually(10.seconds) {
                        lifecycle.isInitialized shouldBe true
                    }
                    lifecycle.initializeCalls.get() shouldBe 3
                }

                lifecycle.stop()
            }

            When("초기화가 계속 실패하는 중에 stop이 호출됨") {
                val lifecycle = FakeLifecycle(failCount = Int.MAX_VALUE)
                lifecycle.start()
                val callsAtStop = lifecycle.initializeCalls.get()
                lifecycle.stop()

                Then("shutdown이 호출되고 재시도가 멈춘다") {
                    lifecycle.shutdownCalls.get() shouldBe 1
                    lifecycle.isRunning shouldBe false
                    lifecycle.isInitialized shouldBe false

                    // 재시도 간격(1초)을 넉넉히 넘겨도 더 이상 호출되지 않는다
                    Thread.sleep(2500)
                    lifecycle.initializeCalls.get() shouldBe callsAtStop
                }
            }

            When("초기화가 진행되는 도중에 stop이 호출됨") {
                val initializeStarted = CountDownLatch(1)
                val stopCalled = CountDownLatch(1)
                val lifecycle =
                    FakeLifecycle(failCount = 0) {
                        initializeStarted.countDown()
                        // stop()이 먼저 실행되도록 초기화를 붙잡아 둔다
                        stopCalled.await(5, TimeUnit.SECONDS)
                    }

                val starter = Thread { lifecycle.start() }.apply { start() }
                initializeStarted.await(5, TimeUnit.SECONDS)
                lifecycle.stop()
                stopCalled.countDown()
                starter.join(5_000)

                Then("뒤늦게 끝난 초기화가 되살린 자원을 다시 정리한다") {
                    lifecycle.isRunning shouldBe false
                    lifecycle.isInitialized shouldBe false
                    lifecycle.shutdownCalls.get() shouldBe 2
                }
            }
        }
    })
