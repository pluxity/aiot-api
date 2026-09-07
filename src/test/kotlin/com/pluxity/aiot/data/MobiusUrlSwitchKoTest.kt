package com.pluxity.aiot.data

import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.pluxity.aiot.mobius.MobiusUrlUpdatedEvent
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

/** 주소를 바꿔도 client가 생성 시점 주소에 묶여 있으면 옛 서버로 계속 동기화한다. */
class MobiusUrlSwitchKoTest :
    BehaviorSpec({
        fun mobiusServer(hits: AtomicInteger): HttpServer =
            HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
                createContext("/") { exchange ->
                    hits.incrementAndGet()
                    val body = """{"m2m:uril":[]}"""
                    exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
                    exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
                    exchange.responseBody.use { it.write(body.toByteArray()) }
                }
                start()
            }

        val oldHits = AtomicInteger()
        val newHits = AtomicInteger()
        val oldServer = mobiusServer(oldHits)
        val newServer = mobiusServer(newHits)
        val factory = RestClientFactory()

        afterSpec {
            factory.destroy()
            oldServer.stop(0)
            newServer.stop(0)
        }

        Given("옛 주소로 만들어진 Mobius 클라이언트") {
            val aiotService =
                AiotService(
                    mockk<FeatureRepository>(relaxed = true),
                    mockk<FeatureQueryService>(relaxed = true),
                    mockk<FeatureStatusWriter>(relaxed = true),
                    mockk<MobiusConfigService> { every { currentUrl } returns "http://127.0.0.1:${oldServer.address.port}" },
                    factory,
                    ServerDomainProperties(url = "http://domain"),
                )

            When("주소 변경 이벤트를 받으면") {
                oldHits.set(0)
                newHits.set(0)
                aiotService.handleMobiusUrlUpdated(MobiusUrlUpdatedEvent("http://127.0.0.1:${newServer.address.port}"))

                Then("새 주소로 동기화한다") {
                    newHits.get() shouldBe 1
                }

                Then("옛 주소로는 더 이상 요청하지 않는다") {
                    oldHits.get() shouldBe 0
                }
            }
        }
    })
