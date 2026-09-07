package com.pluxity.aiot.announcement

import com.influxdb.client.QueryApi
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.properties.LlmProperties
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.web.client.RestClient
import java.time.Duration

/** LLM 생성은 연동 API보다 오래 걸린다. 팩토리 기본값을 그대로 쓰면 느린 생성이 잘린다. */
class LlmClientTimeoutKoTest :
    BehaviorSpec({
        Given("LLM 응답 대기 시간이 설정된 상태") {
            val readTimeout = slot<Long>()
            val restClientFactory: RestClientFactory =
                mockk {
                    every { createClient(any(), any(), capture(readTimeout), any()) } returns RestClient.create()
                }

            When("LLM 클라이언트를 만들면") {
                LlmMessageService(
                    mockk<QueryApi>(),
                    mockk<InfluxdbProperties>(),
                    mockk<LlmMessageRepository>(),
                    mockk<SiteRepository>(),
                    LlmProperties(baseUrl = "http://llm", responseTimeout = Duration.ofMinutes(3)),
                    restClientFactory,
                )

                Then("설정한 값이 그대로 전달된다") {
                    readTimeout.captured shouldBe Duration.ofMinutes(3).toMillis()
                }
            }
        }
    })
