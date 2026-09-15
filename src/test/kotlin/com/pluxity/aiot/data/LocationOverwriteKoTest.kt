package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.DeviceStatus
import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.net.InetSocketAddress

/** 화면에서 손으로 맞춘 위치를 동기화가 다시 지우면 안 된다. 옵션을 끄면 배터리만 갱신한다. */
class LocationOverwriteKoTest :
    BehaviorSpec({
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)

        fun respond(
            path: String,
            body: String,
        ) = server.createContext(path) { exchange ->
            exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
            exchange.sendResponseHeaders(200, body.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(body.toByteArray()) }
        }
        respond("/dev-1/34950_1.0_0/data-report/la", """{"m2m:cin":{"con":{"BatteryLevel":42}}}""")
        respond("/dev-1", """{"m2m:cnt":{"lbl":["latitude: 37.5","longitude: 126.9"]}}""")
        server.start()

        val factory = RestClientFactory()
        val writer = mockk<FeatureStatusWriter>(relaxed = true)
        val aiotService =
            AiotService(
                mockk<FeatureRepository>(relaxed = true),
                mockk<FeatureQueryService> { every { findAllDeviceIds() } returns listOf("dev-1") },
                writer,
                mockk<MobiusConfigService> { every { currentUrl } returns "http://127.0.0.1:${server.address.port}" },
                factory,
                ServerDomainProperties(url = "http://domain"),
            )

        beforeContainer { clearMocks(writer) }
        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        Given("위치 덮어쓰기를 끈 동기화") {
            When("상태를 동기화하면") {
                aiotService.statusSynchronize(overwriteLocation = false)

                Then("위치는 건드리지 않고 배터리만 반영한다") {
                    verify(exactly = 0) { writer.applyStatuses(any()) }
                    verify(exactly = 1) { writer.applyBatteryLevels(mapOf("dev-1" to 42)) }
                }
            }
        }

        Given("기본 동기화") {
            When("상태를 동기화하면") {
                aiotService.statusSynchronize()

                Then("위치와 배터리를 함께 덮어쓴다") {
                    verify(exactly = 1) { writer.applyStatuses(mapOf("dev-1" to DeviceStatus(126.9, 37.5, 42))) }
                }
            }
        }
    })
