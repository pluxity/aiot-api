package com.pluxity.aiot.display

import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk

class DisplayServiceKoTest :
    BehaviorSpec({
        Given("LDMS 연동이 꺼져 있음") {
            val service = DisplayService(null)

            When("목록을 조회함") {
                Then("빈 목록을 반환한다") {
                    service.findAll(null).shouldBeEmpty()
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
            val service = DisplayService(ldmsClient)

            When("전광판 목록을 조회함") {
                val result = service.findAll(null)

                Then("Ok는 정상, Err는 오프라인, 그 외는 확인 불가로 매핑한다") {
                    result.map { it.id to it.status } shouldContainExactly
                        listOf(1L to DeviceStatus.NORMAL, 2L to DeviceStatus.OFFLINE, 3L to DeviceStatus.UNKNOWN)
                }
            }
        }
    })
