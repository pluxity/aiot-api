package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.MobiusContainer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

private fun device(
    ri: String,
    rn: String,
) = MobiusContainer(ri = ri, rn = rn, pi = "AE", lbl = listOf("deviceId:$rn"))

private fun obj(
    pi: String,
    rn: String,
) = MobiusContainer(ri = "$pi-$rn", rn = rn, pi = pi, lbl = listOf("objectVersion:1.0"))

class MobiusDeviceResolverKoTest :
    BehaviorSpec({
        Given("디바이스와 Object Instance 목록") {
            val devices =
                listOf(
                    device("d-thm", "SNIOT-P-THM-018"),
                    device("d-wfl", "SNIOT-P-WFL-013"),
                    device("d-bos", "SNIOT-P-BOS-024"),
                    device("d-none", "SNIOT-P-THM-099"),
                    device("d-tst", "SNIOT-P-TST-001"),
                )
            val objects =
                listOf(
                    obj("d-thm", "34957_1.0_0"),
                    obj("d-thm", "34950_1.0_0"),
                    obj("d-thm", "34954_1.0_0"),
                    obj("d-wfl", "34950_1.0_0"),
                    obj("d-wfl", "34957_1.0_0"),
                    obj("d-bos", "34950_1.0_0"),
                    obj("d-bos", "34957_1.0_0"),
                    obj("d-none", "34950_1.0_0"),
                    obj("d-tst", "34954_1.0_0"),
                )

            When("병합하면") {
                val result = MobiusDeviceResolver.resolve(devices, objects).associate { it.deviceId to it.objectId }

                Then("Object가 여럿이면 deviceId 약어와 맞는 것을 고른다") {
                    result["SNIOT-P-THM-018"] shouldBe "34954_1.0_0"
                    result["SNIOT-P-WFL-013"] shouldBe "34957_1.0_0"
                }

                Then("약어와 맞는 Object가 없으면 아는 Object 중 첫 것을 쓴다") {
                    result["SNIOT-P-BOS-024"] shouldBe "34957_1.0_0"
                }

                Then("공통 Object(34950)만 있는 디바이스와 테스트 장비는 제외한다") {
                    result.containsKey("SNIOT-P-THM-099") shouldBe false
                    result.containsKey("SNIOT-P-TST-001") shouldBe false
                    result.size shouldBe 3
                }
            }
        }
    })
