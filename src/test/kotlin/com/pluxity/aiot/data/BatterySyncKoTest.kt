package com.pluxity.aiot.data

import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import java.net.InetSocketAddress

/**
 * 배터리 값이 없는 것과 요청이 실패한 것은 다르다.
 *
 * 값이 없으면 null로 반영해 옛 수치를 지워야 하고, 요청이 실패했으면 건드리지 말아야 한다.
 * 둘을 뭉뚱그려 빼면 배터리가 빠진 기기가 며칠 전 수치를 계속 표시한다.
 */
class BatterySyncKoTest :
    BehaviorSpec({
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        // 값이 있는 기기
        server.createContext("/has-battery/3_1.2_0/data-report/la") { exchange ->
            val body = """{"m2m:cin":{"con":{"Battery Level":42}}}"""
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        // 배터리 항목이 없어 Mobius가 404를 주는 기기
        server.createContext("/no-battery/3_1.2_0/data-report/la") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()

        val factory = RestClientFactory()

        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        val aiotService =
            AiotService(
                mockk<FeatureRepository>(relaxed = true),
                mockk<FeatureQueryService>(relaxed = true),
                mockk<FeatureStatusWriter>(relaxed = true),
                mockk<MobiusConfigService> { every { currentUrl } returns "http://127.0.0.1:${server.address.port}" },
                factory,
                ServerDomainProperties(url = "http://domain"),
            )

        Given("배터리 값이 있는 기기와 없는 기기") {
            When("배터리를 모아 오면") {
                val levels = aiotService.fetchAllBatteryLevels(listOf("has-battery", "no-battery"))

                Then("값이 없는 기기도 null로 실려 옛 수치가 지워진다") {
                    levels shouldContainExactly mapOf("has-battery" to 42, "no-battery" to null)
                }
            }
        }
    })
