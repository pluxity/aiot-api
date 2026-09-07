package com.pluxity.aiot.eds

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldStartWith
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CompletableFuture

/** 재구독은 공용 parallel 스케줄러에서 일어난다. 거기서 블로킹하면 타이머 워커를 붙든다. */
class EdsWebSocketSchedulerKoTest :
    BehaviorSpec({
        Given("웹소켓 주소 조회가 블로킹 HTTP일 때") {
            val callerThread = CompletableFuture<String>()
            val edsClient: EdsClient =
                mockk {
                    every { getWebSocketUrl() } answers {
                        callerThread.complete(Thread.currentThread().name)
                        "ws://127.0.0.1:1"
                    }
                    every { getApiKey() } returns "key"
                }

            val webSocketClient = EdsWebSocketClient(edsClient, ObjectMapper(), mockk<EdsFacade>())

            When("연결을 시작하면") {
                webSocketClient.connect()
                val threadName = callerThread.get()
                webSocketClient.disconnect()

                Then("블로킹을 허용하는 스케줄러에서 조회한다") {
                    threadName shouldStartWith "boundedElastic"
                }
            }
        }
    })
