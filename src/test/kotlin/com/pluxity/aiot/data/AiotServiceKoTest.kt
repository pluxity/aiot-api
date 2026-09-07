package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.MobiusCntResponse
import com.pluxity.aiot.data.dto.MobiusLocationResponse
import com.pluxity.aiot.feature.FeatureQueryService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.feature.FeatureStatusWriter
import com.pluxity.aiot.global.config.RestClientFactory
import com.pluxity.aiot.global.properties.ServerDomainProperties
import com.pluxity.aiot.mobius.MobiusConfigService
import com.pluxity.aiot.sensor.type.SensorType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper

class AiotServiceKoTest :
    BehaviorSpec({
        val jsonMapper = JsonMapper.builder().build()

        fun createServiceWithResponse(responseBody: Any): AiotService {
            val builder = RestClient.builder()
            MockRestServiceServer
                .bindTo(builder)
                .build()
                .expect(ExpectedCount.manyTimes(), anything())
                .andRespond(withSuccess(jsonMapper.writeValueAsString(responseBody), MediaType.APPLICATION_JSON))

            val restClientFactory: RestClientFactory =
                mockk {
                    every { createClient(any(), any(), any(), any()) } returns builder.build()
                }

            val mobiusConfigService: MobiusConfigService =
                mockk {
                    every { currentUrl } returns "http://mobius"
                }

            return AiotService(
                mockk<FeatureRepository>(relaxed = true),
                mockk<FeatureQueryService>(relaxed = true),
                mockk<FeatureStatusWriter>(relaxed = true),
                mobiusConfigService,
                restClientFactory,
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
