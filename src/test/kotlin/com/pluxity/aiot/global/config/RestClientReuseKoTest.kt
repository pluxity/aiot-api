package com.pluxity.aiot.global.config

import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.ints.shouldBeLessThan
import java.net.InetSocketAddress

/** URL 변경 이벤트마다 클라이언트를 새로 만든다. 매번 새 HttpClient를 물면 셀렉터 스레드가 쌓인다. */
class RestClientReuseKoTest :
    BehaviorSpec({
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val body = "ok"
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        val baseUrl = "http://127.0.0.1:${server.address.port}"

        val factory = RestClientFactory()

        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        fun selectorThreads() = Thread.getAllStackTraces().keys.count { it.name.contains("SelectorManager") }

        Given("전송 설정이 같은 클라이언트를 반복해서 만드는 상황") {
            When("20번 만들어 각각 요청을 보내면") {
                val before = selectorThreads()
                repeat(20) {
                    factory
                        .createClient(baseUrl)
                        .get()
                        .uri("/")
                        .retrieve()
                        .body(String::class.java)
                }
                val added = selectorThreads() - before

                Then("바탕 HttpClient를 나눠 써 스레드가 쌓이지 않는다") {
                    added shouldBeLessThan 5
                }
            }
        }
    })
