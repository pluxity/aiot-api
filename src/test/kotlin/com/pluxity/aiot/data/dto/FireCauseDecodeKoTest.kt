package com.pluxity.aiot.data.dto

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Instant

class FireCauseDecodeKoTest :
    BehaviorSpec({
        Given("FireCauseMask 비트 마스크") {
            When("3 (온도 이상 + CO 이상)") {
                Then("두 원인으로 디코딩된다") {
                    FireCause.decode(3).map { it.description } shouldBe listOf("온도 이상", "CO 이상")
                }
            }

            When("0") {
                Then("원인이 없다") {
                    FireCause.decode(0) shouldBe emptyList()
                }
            }

            When("31 (모든 비트)") {
                Then("모든 원인으로 디코딩된다") {
                    FireCause.decode(31) shouldBe FireCause.entries
                }
            }
        }

        Given("산불 감지기 최신값 응답") {
            When("FireCauseMask = 24 (습도 이상 + CO2 이상)인 데이터를 응답으로 변환") {
                val response =
                    ForestFireSensorData(
                        time = Instant.parse("2026-05-11T01:00:00Z"),
                        fireDetection = 1.0,
                        temperature = 35.0,
                        fireCauseMask = 24.0,
                    ).toDeviceDataResponse("FFA_001")

                Then("FireCauseMask 메트릭에 원인 목록이 함께 담긴다") {
                    val metric = response.metrics["FireCauseMask"]
                    metric?.value shouldBe 24.0
                    metric?.causes shouldBe listOf("습도 이상", "CO2 이상")
                }

                Then("다른 메트릭에는 원인 목록이 없다") {
                    response.metrics["Temperature"]?.value shouldBe 35.0
                    response.metrics["Temperature"]?.causes.shouldBeNull()
                }
            }
        }
    })
