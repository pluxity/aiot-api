package com.pluxity.aiot.data

import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.InetSocketAddress
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

class MobiusRequestHeadersKoTest :
    BehaviorSpec({
        val received = CopyOnWriteArrayList<Map<String, String?>>()
        val server =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/") { exchange ->
                    received +=
                        listOf("X-M2M-RI", "X-M2M-Origin", "Accept").associateWith { exchange.requestHeaders.getFirst(it) }
                    val body = """{"m2m:uril":["Mobius/AE/SNIOT-P-WFL-001/34957_1.0_0"]}"""
                    exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
                    exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
                    exchange.responseBody.use { it.write(body.toByteArray()) }
                }
                start()
            }
        val factory = RestClientFactory()

        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        Given("Mobius 클라이언트") {
            val aiotService =
                AiotService(
                    mockk<FeatureRepository>(relaxed = true),
                    mockk<FeatureQueryService>(relaxed = true),
                    mockk<FeatureStatusWriter>(relaxed = true),
                    mockk<MobiusConfigService> { every { currentUrl } returns "http://127.0.0.1:${server.address.port}" },
                    factory,
                    ServerDomainProperties(url = "http://domain"),
                )

            When("동기화 요청을 보내면") {
                val before = Instant.now().epochSecond
                aiotService.checkSynchronization()
                val after = Instant.now().epochSecond
                val headers = received.single()

                Then("규격 공통 헤더를 붙인다") {
                    headers["X-M2M-Origin"] shouldBe "S_AIoT_Application"
                    headers["Accept"] shouldBe "application/json"
                }

                Then("X-M2M-RI는 요청 시각의 Unix timestamp다") {
                    headers["X-M2M-RI"]!!.toLong() shouldBeInRange before..after
                }
            }
        }
    })
