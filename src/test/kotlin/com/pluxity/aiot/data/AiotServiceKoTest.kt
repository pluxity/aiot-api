package com.pluxity.aiot.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.data.dto.MobiusCntResponse
import com.pluxity.aiot.data.dto.MobiusLocationResponse
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.config.WebClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class AiotServiceKoTest :
    BehaviorSpec({
        fun createServiceWithResponse(responseBody: Any): AiotService {
            val objectMapper = ObjectMapper()

            val exchangeFunction =
                ExchangeFunction {
                    // responseBody 객체를 JSON 문자열로 직렬화
                    val json = objectMapper.writeValueAsString(responseBody)

                    val response =
                        ClientResponse
                            .create(HttpStatus.OK)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .body(json)
                            .build()
                    Mono.just(response)
                }

            val webClient =
                WebClient
                    .builder()
                    .exchangeFunction(exchangeFunction)
                    .build()

            val webClientFactory: WebClientFactory =
                mockk {
                    every { createClient(any(), any(), any(), any()) } returns webClient
                }

            val featureRepository: FeatureRepository = mockk(relaxed = true)
            val siteRepository: SiteRepository = mockk(relaxed = true)
            val mobiusConfigService: MobiusConfigService =
                mockk {
                    every { currentUrl } returns "http://mobius"
                }

            return AiotService(
                featureRepository,
                siteRepository,
                mobiusConfigService,
                webClientFactory,
                ServerDomainProperties(url = "http://domain"),
            )
        }

        Given("parseDeviceId") {
            val aiotService = createServiceWithResponse(MobiusLocationResponse(MobiusCntResponse(emptyList())))
            When("약어가 존재하고 숫자 식별자가 포함된 경우") {
                val deviceType = SensorType.fromObjectId(SensorType.TEMPERATURE_HUMIDITY.objectId)
                val abbrMap = mapOf(deviceType.abbreviation.abbreviationKey to deviceType.abbreviation)
                val result = aiotService.parseDeviceId("SNIOT-THM-001", abbrMap)
                Then("풀네임과 식별자를 결합해 반환한다") {
                    result shouldBe "온습도계-001"
                }
            }

            When("정의된 약어가 없으면") {
                val result = aiotService.parseDeviceId("UNKNOWN-123", emptyMap())
                Then("원본 deviceId를 유지한다") {
                    result shouldBe "UNKNOWN-123"
                }
            }
        }

        Given("fetchDeviceLocationData") {
            val responseWithCoordinates =
                MobiusLocationResponse(
                    MobiusCntResponse(
                        listOf(
                            "Latitude: 37.5",
                            "Longitude: 127.03",
                        ),
                    ),
                )

            val responseWithoutLongitude =
                MobiusLocationResponse(
                    MobiusCntResponse(listOf("latitude: 37.5")),
                )

            When("위치 정보가 모두 포함되면") {
                val service = createServiceWithResponse(responseWithCoordinates)
                val result = service.fetchDeviceLocationData("device-1")
                Then("LocationData를 반환한다") {
                    result?.latitude shouldBe 37.5
                    result?.longitude shouldBe 127.03
                }
            }

            When("필수 좌표가 누락되면") {
                val service = createServiceWithResponse(responseWithoutLongitude)
                val result = service.fetchDeviceLocationData("device-2")
                Then("null을 반환한다") {
                    result.shouldBeNull()
                }
            }
        }
    })
