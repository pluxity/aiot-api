package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

private val umsProperties = UmsProperties(enabled = true, senderNumber = "032-000-0000")

class SmsServiceKoTest :
    BehaviorSpec({

        Given("문자 발송") {
            When("여러 대상에게 발송하고 모두 성공") {
                val sender: SmsSender = mockk()
                val repository: SmsHistoryRepository = mockk()
                every { sender.send(any()) } returnsMany
                    listOf(
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 1L),
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 2L),
                    )
                val saved = mutableListOf<SmsHistory>()
                every { repository.save(capture(saved)) } answers { saved.last() }

                val result =
                    SmsService(sender, repository, umsProperties)
                        .send("이벤트 알림", "화재 감지", listOf("01011112222", "01033334444"))

                Then("대상 수만큼 이력이 저장된다") {
                    result shouldHaveSize 2
                    saved.map { it.targetNumber } shouldBe listOf("01011112222", "01033334444")
                    saved.map { it.clidx } shouldBe listOf(1L, 2L)
                    saved.all { it.stat == UmsSendStat.SUCCESS } shouldBe true
                    saved.all { it.senderNumber == "032-000-0000" } shouldBe true
                }
            }

            When("표기만 다른 같은 번호가 섞여 있음") {
                val sender: SmsSender = mockk()
                val repository: SmsHistoryRepository = mockk()
                every { sender.send(any()) } returns SmsSendResult(UmsSendStat.SUCCESS, clidx = 1L)
                val saved = mutableListOf<SmsHistory>()
                every { repository.save(capture(saved)) } answers { saved.last() }

                SmsService(sender, repository, umsProperties)
                    .send("제목", "내용", listOf("010-1111-2222", "01011112222", "010-1111-2222"))

                Then("한 번만 발송하고 원본 표기를 유지한다") {
                    verify(exactly = 1) { sender.send(any()) }
                    saved shouldHaveSize 1
                    saved.first().targetNumber shouldBe "010-1111-2222"
                }
            }

            When("일부 대상 발송이 실패") {
                val sender: SmsSender = mockk()
                val repository: SmsHistoryRepository = mockk()
                every { sender.send(any()) } returnsMany
                    listOf(
                        SmsSendResult(UmsSendStat.NO_ACCOUNT, failureReason = "계정없음"),
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 5L),
                    )
                val saved = mutableListOf<SmsHistory>()
                every { repository.save(capture(saved)) } answers { saved.last() }

                SmsService(sender, repository, umsProperties)
                    .send("제목", "내용", listOf("01011112222", "01033334444"))

                Then("실패 건도 사유와 함께 이력으로 남고 나머지 발송은 계속된다") {
                    saved shouldHaveSize 2
                    saved[0].stat shouldBe UmsSendStat.NO_ACCOUNT
                    saved[0].failureReason shouldBe "계정없음"
                    saved[0].clidx.shouldBeNull()
                    saved[1].stat shouldBe UmsSendStat.SUCCESS
                }
            }
        }

        Given("미연동 환경") {
            When("LoggingSmsSender로 발송") {
                val result = LoggingSmsSender().send(SmsSendRequest("제목", "내용", "01011112222"))

                Then("실제 발송 없이 미연동으로 표시된다") {
                    result.stat shouldBe UmsSendStat.NOT_SENT
                    result.clidx.shouldBeNull()
                    result.failureReason shouldBe "UMS 미연동"
                }
            }
        }
    })
