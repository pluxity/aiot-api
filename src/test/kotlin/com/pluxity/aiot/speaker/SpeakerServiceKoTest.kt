package com.pluxity.aiot.speaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SpeakerServiceKoTest :
    BehaviorSpec({
        val objectMapper = ObjectMapper()

        Given("LDMS 연동이 꺼져 있음") {
            val service = SpeakerService(null, objectMapper)

            When("스피커 목록을 조회함") {
                Then("빈 목록을 반환한다") {
                    service.findAll(null).shouldBeEmpty()
                }
            }
        }

        Given("LDMS 송출그룹 상태가 JSON 문자열로 옴") {
            val ldmsClient = mockk<LdmsClient>()
            every { ldmsClient.getEmcallGroupList() } returns
                listOf(
                    LdmsEmcallGroupInfo(
                        emcallGrpSeq = 1,
                        commStat = "Ok",
                        emcallGrpStatJson = """{"device_id":"901","msg":"On","tts_msg":"IDLE","play":"0"}""",
                    ),
                    LdmsEmcallGroupInfo(emcallGrpSeq = 2, emcallGrpStatJson = "{broken"),
                    LdmsEmcallGroupInfo(emcallGrpSeq = 3, emcallGrpStatJson = null),
                )
            val service = SpeakerService(ldmsClient, objectMapper)

            When("스피커 목록을 조회함") {
                val result = service.findAll(null)

                Then("파싱되면 출력 상태를 채우고, 깨졌거나 없으면 null로 둔다") {
                    result[0].status shouldBe DeviceStatus.NORMAL
                    result[0].output?.message shouldBe "On"
                    result[0].output?.ttsMessage shouldBe "IDLE"
                    result[1].output shouldBe null
                    result[2].output shouldBe null
                }
            }
        }
    })
