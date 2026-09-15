package com.pluxity.aiot.data.subscription

import com.pluxity.aiot.global.config.RestClientFactory
import com.sun.net.httpserver.HttpServer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** 내부망 운영 서버가 받은 Mobius 알림을 개발 서버로 그대로 넘기는 임시 기능. */
@Configuration
@EnableConfigurationProperties(SubscriptionForwardProperties::class)
@Import(SubscriptionForwardFilter::class)
private class ForwardFilterConfig

class SubscriptionForwardFilterKoTest :
    BehaviorSpec({
        val received = LinkedBlockingQueue<String>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/subscription") { exchange ->
            received.add(exchange.requestBody.readAllBytes().decodeToString())
            exchange.sendResponseHeaders(200, -1)
            exchange.close()
        }
        server.start()
        val devUrl = "http://127.0.0.1:${server.address.port}"

        val factory = RestClientFactory()
        afterSpec {
            factory.destroy()
            server.stop(0)
        }

        // 컨트롤러처럼 본문을 끝까지 읽는다. 읽지 않으면 캐시에 아무 것도 남지 않는다
        val consumingController =
            object : HttpServlet() {
                override fun doPost(
                    req: HttpServletRequest,
                    resp: HttpServletResponse,
                ) {
                    req.inputStream.readAllBytes()
                }
            }

        fun mobiusNotification(body: String) =
            MockHttpServletRequest("POST", "/subscription").apply {
                contentType = "application/json"
                setContent(body.toByteArray())
            }

        Given("전달 주소가 설정된 운영 서버") {
            val filter = SubscriptionForwardFilter(SubscriptionForwardProperties(url = devUrl), factory)

            When("Mobius 알림이 들어오면") {
                val body = """{"m2m:sgn":{"sur":"Mobius/x/SNIOT-P-WFL-001/34957_1.0_0/data-report/s"}}"""
                filter.doFilter(mobiusNotification(body), MockHttpServletResponse(), MockFilterChain(consumingController))

                Then("본문이 그대로 개발 서버에 도착한다") {
                    received.poll(5, TimeUnit.SECONDS) shouldBe body
                }
            }

            When("다른 경로의 POST가 들어오면") {
                val request = MockHttpServletRequest("POST", "/features/sync").apply { setContent("{}".toByteArray()) }
                filter.doFilter(request, MockHttpServletResponse(), MockFilterChain(consumingController))

                Then("전달하지 않는다") {
                    received.poll(1, TimeUnit.SECONDS).shouldBeNull()
                }
            }
        }

        Given("필터 빈 등록 조건") {
            val contextRunner =
                ApplicationContextRunner()
                    .withBean(RestClientFactory::class.java)
                    .withUserConfiguration(ForwardFilterConfig::class.java)

            When("전달 주소 프로퍼티가 없으면") {
                Then("필터 빈이 만들어지지 않는다") {
                    contextRunner.run { context ->
                        context.getBeansOfType(SubscriptionForwardFilter::class.java).shouldBeEmpty()
                    }
                }
            }

            When("전달 주소가 빈 문자열이면") {
                Then("필터 빈이 만들어지지 않는다") {
                    contextRunner.withPropertyValues("subscription.forward.url=").run { context ->
                        context.getBeansOfType(SubscriptionForwardFilter::class.java).shouldBeEmpty()
                    }
                }
            }

            When("전달 주소 프로퍼티가 있으면") {
                Then("필터 빈이 만들어진다") {
                    contextRunner.withPropertyValues("subscription.forward.url=$devUrl").run { context ->
                        context.getBeansOfType(SubscriptionForwardFilter::class.java) shouldHaveSize 1
                    }
                }
            }
        }
    })
