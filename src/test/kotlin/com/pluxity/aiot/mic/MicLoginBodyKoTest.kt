package com.pluxity.aiot.mic

import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.MicProperties
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture

/** 업체 서버가 본문 누락으로 422를 준다. 실제로 나가는 바이트를 받아 확인한다. */
class MicLoginBodyKoTest :
    BehaviorSpec({
        val received = CompletableFuture<String>()
        val contentType = CompletableFuture<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/v1/auth/token") { exchange ->
            contentType.complete(exchange.requestHeaders.getFirst("Content-Type") ?: "(없음)")
            received.complete(exchange.requestBody.readBytes().decodeToString())
            val body = """{"access_token":"t","expires_in":60}"""
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()

        val factory = RestClientFactory()
        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        Given("자격증명으로 로그인할 때") {
            val micClient =
                MicClient(
                    factory,
                    MicProperties(
                        enabled = true,
                        baseUrl = "http://127.0.0.1:${server.address.port}",
                        username = "api",
                        password = "pw",
                    ),
                )

            When("로그인을 부르면") {
                micClient.login()

                Then("자격증명이 본문에 실린다") {
                    received.get() shouldBe """{"username":"api","password":"pw"}"""
                }

                Then("JSON으로 보낸다") {
                    contentType.get() shouldContain "application/json"
                }
            }
        }
    })
