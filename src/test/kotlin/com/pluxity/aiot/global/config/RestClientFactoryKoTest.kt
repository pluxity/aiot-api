package com.pluxity.aiot.global.config

import com.sun.net.httpserver.HttpServer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.net.InetSocketAddress

/** 응답 본문의 코드로 성패를 판정하는 연동처가 있어, 상태코드 처리 방식이 클라이언트마다 갈린다. */
class RestClientFactoryKoTest :
    BehaviorSpec({
        val received = mutableMapOf<String, String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange ->
            received["accept"] = exchange.requestHeaders.getFirst("Accept") ?: ""
            val body = """{"result":"ok"}"""
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/slow") { exchange ->
            Thread.sleep(500)
            val body = """{"result":"late"}"""
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.createContext("/fail") { exchange ->
            val body = """{"code":"E01","message":"업체 규약상 본문으로 판정"}"""
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(500, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        server.start()
        val baseUrl = "http://127.0.0.1:${server.address.port}"

        val factory = RestClientFactory()

        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        Given("팩토리 기본값으로 만든 클라이언트") {
            val client = factory.createClient(baseUrl)

            When("요청을 보내면") {
                val body =
                    client
                        .get()
                        .uri("/ok")
                        .retrieve()
                        .body(String::class.java)

                Then("응답을 받는다") {
                    body shouldBe """{"result":"ok"}"""
                }

                Then("Accept를 json으로 실어 보낸다") {
                    received["accept"] shouldBe "application/json"
                }
            }

            When("서버가 5xx를 주면") {
                Then("예외를 던진다") {
                    shouldThrow<RestClientResponseException> {
                        client
                            .get()
                            .uri("/fail")
                            .retrieve()
                            .body(String::class.java)
                    }
                }
            }
        }

        Given("응답이 느린 서버") {
            When("읽기 대기 시간을 짧게 준 클라이언트로 부르면") {
                val impatient = factory.createClient(baseUrl, readTimeoutMs = 100)

                Then("기다리지 않고 끊는다") {
                    shouldThrow<ResourceAccessException> {
                        impatient
                            .get()
                            .uri("/slow")
                            .retrieve()
                            .body(String::class.java)
                    }
                }
            }

            When("넉넉히 준 클라이언트로 부르면") {
                val patient = factory.createClient(baseUrl, readTimeoutMs = 5000)

                Then("응답을 끝까지 받는다") {
                    patient
                        .get()
                        .uri("/slow")
                        .retrieve()
                        .body(String::class.java) shouldBe """{"result":"late"}"""
                }
            }
        }

        Given("상태코드로 판정하지 않는 클라이언트") {
            val client = factory.createClient(baseUrl, throwOnHttpError = false)

            When("서버가 5xx를 주면") {
                val body =
                    client
                        .get()
                        .uri("/fail")
                        .retrieve()
                        .body(String::class.java)

                Then("예외 대신 본문을 그대로 돌려준다") {
                    body shouldBe """{"code":"E01","message":"업체 규약상 본문으로 판정"}"""
                }
            }
        }
    })
