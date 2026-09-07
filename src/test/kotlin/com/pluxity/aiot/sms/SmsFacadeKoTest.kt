package com.pluxity.aiot.sms

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsSendRequest
import com.pluxity.aiot.sms.dto.SmsSendResult
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

private val umsProperties = UmsProperties(enabled = true, senderNumber = "032-000-0000")

class SmsFacadeKoTest :
    BehaviorSpec({

        Given("문자 발송") {
            When("여러 대상에게 발송하고 모두 성공") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()
                every { sender.send(any()) } returnsMany
                    listOf(
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 1L),
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 2L),
                    )
                val saved = slot<List<SmsHistory>>()
                every { historyService.saveAll(capture(saved)) } answers { saved.captured }

                val result =
                    SmsFacade(sender, historyService, umsProperties)
                        .send("이벤트 알림", "화재 감지", listOf("01011112222", "01033334444"))

                Then("대상 수만큼 이력이 저장된다") {
                    result shouldHaveSize 2
                    result.map { it.clidx } shouldBe listOf(1L, 2L)
                    saved.captured.map { it.targetNumber } shouldBe listOf("01011112222", "01033334444")
                    saved.captured.all { it.stat == UmsSendStat.SUCCESS } shouldBe true
                    saved.captured.all { it.senderNumber == "032-000-0000" } shouldBe true
                }
            }

            When("표기만 다른 같은 번호가 섞여 있음") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()
                every { sender.send(any()) } returns SmsSendResult(UmsSendStat.SUCCESS, clidx = 1L)
                val saved = slot<List<SmsHistory>>()
                every { historyService.saveAll(capture(saved)) } answers { saved.captured }

                SmsFacade(sender, historyService, umsProperties)
                    .send("제목", "내용", listOf("010-1111-2222", "01011112222", "010-1111-2222"))

                Then("한 번만 발송한다") {
                    verify(exactly = 1) { sender.send(any()) }
                    saved.captured shouldHaveSize 1
                    saved.captured.first().targetNumber shouldBe "010-1111-2222"
                }
            }

            When("숫자가 하나도 없는 서로 다른 값") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()
                every { sender.send(any()) } returns SmsSendResult(UmsSendStat.NOT_SENT, failureReason = "형식 오류")
                val saved = slot<List<SmsHistory>>()
                every { historyService.saveAll(capture(saved)) } answers { saved.captured }

                SmsFacade(sender, historyService, umsProperties).send("제목", "내용", listOf("없음", "미상", "없음"))

                Then("한 건으로 뭉개지 않고 값별로 이력을 남긴다") {
                    saved.captured.map { it.targetNumber } shouldBe listOf("없음", "미상")
                }
            }

            When("일부 대상 발송이 실패") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()
                every { sender.send(any()) } returnsMany
                    listOf(
                        SmsSendResult(UmsSendStat.NO_ACCOUNT, failureReason = "계정없음"),
                        SmsSendResult(UmsSendStat.SUCCESS, clidx = 5L),
                    )
                val saved = slot<List<SmsHistory>>()
                every { historyService.saveAll(capture(saved)) } answers { saved.captured }

                SmsFacade(sender, historyService, umsProperties)
                    .send("제목", "내용", listOf("01011112222", "01033334444"))

                Then("실패 건도 사유와 함께 이력으로 남고 나머지 발송은 계속된다") {
                    saved.captured shouldHaveSize 2
                    saved.captured[0].stat shouldBe UmsSendStat.NO_ACCOUNT
                    saved.captured[0].failureReason shouldBe "계정없음"
                    saved.captured[0].clidx.shouldBeNull()
                    saved.captured[1].stat shouldBe UmsSendStat.SUCCESS
                }
            }

            When("발송 후 이력 저장이 실패") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()
                every { sender.send(any()) } returns SmsSendResult(UmsSendStat.SUCCESS, clidx = 1L)
                every { historyService.saveAll(any<List<SmsHistory>>()) } throws IllegalStateException("DB 오류")

                Then("발송 실패와 구분되는 오류로 알려 재시도를 막는다") {
                    val exception =
                        shouldThrowExactly<CustomException> {
                            SmsFacade(sender, historyService, umsProperties).send("제목", "내용", listOf("01011112222"))
                        }
                    exception.errorCode shouldBe ErrorCode.SMS_HISTORY_SAVE_FAILED
                }
            }
        }

        Given("제목·내용이 유효하지 않은 요청") {
            When("내용이 2000자를 초과") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()

                Then("예외를 던지고 아무에게도 발송하지 않는다") {
                    val exception =
                        shouldThrowExactly<CustomException> {
                            SmsFacade(sender, historyService, umsProperties)
                                .send("제목", "가".repeat(2001), listOf("01011112222", "01033334444"))
                        }
                    exception.errorCode shouldBe ErrorCode.SMS_INVALID_CONTENT
                    verify(exactly = 0) { sender.send(any()) }
                    verify(exactly = 0) { historyService.saveAll(any<List<SmsHistory>>()) }
                }
            }

            When("제목이 50자를 초과") {
                val sender: SmsSender = mockk()
                val historyService: SmsHistoryService = mockk()

                Then("발송을 시작하지 않는다") {
                    shouldThrowExactly<CustomException> {
                        SmsFacade(sender, historyService, umsProperties)
                            .send("가".repeat(51), "내용", listOf("01011112222"))
                    }
                    verify(exactly = 0) { sender.send(any()) }
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
