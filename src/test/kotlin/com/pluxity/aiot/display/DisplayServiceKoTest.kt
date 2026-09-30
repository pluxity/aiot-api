package com.pluxity.aiot.display

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class DisplayServiceKoTest :
    BehaviorSpec({
        val objectMapper = ObjectMapper()

        Given("LDMS 연동이 꺼져 있음") {
            val service = DisplayService(null, objectMapper)

            When("목록을 조회함") {
                Then("빈 목록을 반환한다") {
                    service.findAll(null).shouldBeEmpty()
                    service.findBroadcastGroups().shouldBeEmpty()
                }
            }
        }

        Given("LDMS 전광판의 통신 상태가 제각각임") {
            val ldmsClient = mockk<LdmsClient>()
            every { ldmsClient.getEbrdList() } returns
                listOf(
                    LdmsEbrdInfo(ebrdSeq = 1, ebrdId = "A", ebrdNm = "정문", commStat = "Ok"),
                    LdmsEbrdInfo(ebrdSeq = 2, ebrdId = "B", ebrdNm = "후문", commStat = "Err"),
                    LdmsEbrdInfo(ebrdSeq = 3, ebrdId = "C", ebrdNm = "주차장", commStat = null),
                )
            val service = DisplayService(ldmsClient, objectMapper)

            When("전광판 목록을 조회함") {
                val result = service.findAll(null)

                Then("Ok는 정상, Err는 오프라인, 그 외는 확인 불가로 매핑한다") {
                    result.map { it.id to it.status } shouldContainExactly
                        listOf(1L to DeviceStatus.NORMAL, 2L to DeviceStatus.OFFLINE, 3L to DeviceStatus.UNKNOWN)
                }
            }
        }

        Given("LDMS 송출그룹 상태가 JSON 문자열로 옴") {
            val ldmsClient = mockk<LdmsClient>()
            every { ldmsClient.getEmcallGroupList() } returns
                listOf(
                    LdmsEmcallGroupInfo(
                        emcallGrpSeq = 1,
                        emcallGrpStatJson = """{"device_id":"901","msg":"On","tts_msg":"IDLE","play":"0"}""",
                    ),
                    LdmsEmcallGroupInfo(emcallGrpSeq = 2, emcallGrpStatJson = "{broken"),
                    LdmsEmcallGroupInfo(emcallGrpSeq = 3, emcallGrpStatJson = null),
                )
            val service = DisplayService(ldmsClient, objectMapper)

            When("송출그룹 목록을 조회함") {
                val result = service.findBroadcastGroups()

                Then("파싱되면 출력 상태를 채우고, 깨졌거나 없으면 null로 둔다") {
                    result[0].output?.message shouldBe "On"
                    result[0].output?.ttsMessage shouldBe "IDLE"
                    result[1].output shouldBe null
                    result[2].output shouldBe null
                }
            }
        }
    })
