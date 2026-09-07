package com.pluxity.aiot.eds

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.global.properties.EdsProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.ServerSocket
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** 재구독은 공용 parallel 스케줄러에서 일어난다. 거기서 블로킹하면 타이머 워커를 붙든다. */
class EdsWebSocketSchedulerKoTest :
    BehaviorSpec({
        val closedPort = ServerSocket(0).use { it.localPort }

        Given("연결이 실패해 재연결을 반복하는 상황") {
            val threads = CopyOnWriteArrayList<String>()
            val twoAttempts = CountDownLatch(2)

            val edsClient: EdsClient =
                mockk {
                    every { getWebSocketUrl() } answers {
                        threads += Thread.currentThread().name
                        twoAttempts.countDown()
                        "ws://127.0.0.1:$closedPort"
                    }
                    every { getApiKey() } returns "key"
                }

            val webSocketClient =
                EdsWebSocketClient(
                    edsClient,
                    ObjectMapper(),
                    mockk<EdsFacade>(),
                    EdsProperties(reconnectDelay = Duration.ofMillis(100)),
                )

            When("두 번째 연결 시도까지 지켜보면") {
                webSocketClient.connect()
                val reached = twoAttempts.await(10, TimeUnit.SECONDS)
                webSocketClient.disconnect()

                Then("재연결이 실제로 일어난다") {
                    reached shouldBe true
                    threads shouldHaveAtLeastSize 2
                }

                Then("최초 연결도 재연결도 블로킹을 허용하는 스케줄러에서 조회한다") {
                    threads.all { it.startsWith("boundedElastic") } shouldBe true
                }
            }
        }
    })
