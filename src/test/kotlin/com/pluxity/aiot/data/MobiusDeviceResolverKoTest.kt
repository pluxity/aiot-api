package com.pluxity.aiot.data

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private const val AE = "Mobius/SLENO-NC.SNIOT_pluxity"

class MobiusDeviceResolverKoTest :
    BehaviorSpec({
        Given("Object Instance 검색 경로 목록") {
            val uril =
                listOf(
                    "$AE/SNIOT-P-WFL-013/34950_1.0_0",
                    "$AE/SNIOT-P-WFL-013/34954_1.0_0",
                    "$AE/SNIOT-P-WFL-013/34957_1.0_0",
                    "$AE/SNIOT-P-BOS-024/34950_1.0_0",
                    "$AE/SNIOT-P-BOS-024/34957_1.0_0",
                    "$AE/SNIOT-P-FFA-099/34950_1.0_0",
                    "$AE/SNIOT-P-THM-018/34954_1.0_0",
                    "$AE/SNIOT-P-TST-001/34954_1.0_0",
                    "$AE/SNIOT-P-WFL-013",
                    "$AE/SNIOT-P-WFL-013/34957_1.0_0/data-report",
                )

            When("병합하면") {
                val result = MobiusDeviceResolver.resolve(uril).associate { it.deviceId to it.objectId }

                Then("Object가 여럿이면 deviceId 약어와 맞는 것을 고른다") {
                    result["SNIOT-P-WFL-013"] shouldBe "34957_1.0_0"
                }

                Then("약어와 맞는 Object가 없으면 아는 Object 중 첫 것을 쓴다") {
                    result["SNIOT-P-BOS-024"] shouldBe "34957_1.0_0"
                }

                Then("공통 Object(34950)만 있는 디바이스, 테스트 장비, 온습도계, 깊이가 다른 경로는 제외한다") {
                    result.containsKey("SNIOT-P-FFA-099") shouldBe false
                    result.containsKey("SNIOT-P-TST-001") shouldBe false
                    result.containsKey("SNIOT-P-THM-018") shouldBe false
                    result.size shouldBe 2
                }
            }
        }
    })
