package com.pluxity.aiot.global.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.net.ServerSocket
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

/** 업체 서버가 본문을 못 받아 422를 냈다. 나가는 요청의 전송 형태를 바이트로 못박는다. */
class RestClientWireFormatKoTest :
    BehaviorSpec({
        val raw = CompletableFuture<String>()
        val server = ServerSocket(0)
        thread(isDaemon = true) {
            server.accept().use { socket ->
                val buf = ByteArray(4096)
                val read = socket.getInputStream().read(buf)
                raw.complete(String(buf, 0, read))
                socket.getOutputStream().write(
                    "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 2\r\n\r\n{}".toByteArray(),
                )
                socket.getOutputStream().flush()
            }
        }

        val factory = RestClientFactory()
        afterSpec {
            factory.destroy()
            server.close()
        }

        Given("본문이 있는 POST 요청") {
            When("팩토리가 만든 클라이언트로 보내면") {
                runCatching {
                    factory
                        .createClient("http://127.0.0.1:${server.localPort}")
                        .post()
                        .uri("/auth/token")
                        .body(mapOf("username" to "api"))
                        .retrieve()
                        .body(String::class.java)
                }
                val request = raw.get()

                Then("길이를 밝혀 보낸다") {
                    request shouldContain "Content-Length: ${"""{"username":"api"}""".length}"
                }

                Then("길이를 모르는 채 흘려보내지 않는다") {
                    request.lowercase().contains("transfer-encoding: chunked") shouldBe false
                }

                Then("평문 연결에 HTTP/2 업그레이드를 붙이지 않는다") {
                    request.lowercase().contains("upgrade: h2c") shouldBe false
                }
            }
        }
    })
