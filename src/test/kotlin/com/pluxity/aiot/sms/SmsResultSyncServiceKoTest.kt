package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.PendingSmsResult
import com.pluxity.aiot.sms.dto.UmsSendResultRow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

private val umsProperties = UmsProperties(enabled = true, resultPollBatchSize = 10)

private fun history(clidx: Long?) =
    SmsHistory(
        targetNumber = "01011112222",
        title = "제목",
        message = "내용",
        stat = UmsSendStat.SUCCESS,
        clidx = clidx,
    )

class SmsResultSyncServiceKoTest :
    BehaviorSpec({

        Given("발송 결과 동기화") {
            When("결과가 확정된 건을 조회함") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk()
                every { historyService.findPending(any(), any()) } returns listOf(PendingSmsResult(1L, 10L))
                every { client.findSendResult(10L) } returns
                    UmsSendResultRow(
                        resultCode = 903,
                        statusCode = 333,
                        errorCode = "0",
                        messageType = "S",
                        completedAt = LocalDateTime.of(2026, 9, 3, 10, 0),
                    )
                val applied = slot<Map<Long, UmsSendResultRow?>>()
                every { historyService.applyResults(capture(applied)) } returns 1

                val confirmed = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("조회한 행이 이력 갱신으로 넘어간다") {
                    confirmed shouldBe 1
                    applied.captured.keys shouldContainExactly setOf(1L)
                    applied.captured[1L].shouldNotBeNull().resultCode shouldBe 903
                }
            }

            When("아직 결과가 없는 건") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk()
                every { historyService.findPending(any(), any()) } returns listOf(PendingSmsResult(2L, 11L))
                every { client.findSendResult(11L) } returns null
                val applied = slot<Map<Long, UmsSendResultRow?>>()
                every { historyService.applyResults(capture(applied)) } returns 0

                val confirmed = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("결과는 비어 있지만 조회 대상으로는 넘겨 시각을 남긴다") {
                    confirmed shouldBe 0
                    applied.captured.keys shouldContainExactly setOf(2L)
                    applied.captured[2L].shouldBeNull()
                }
            }

            When("한 건 조회가 예외를 던짐") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk()
                every { historyService.findPending(any(), any()) } returns
                    listOf(PendingSmsResult(3L, 12L), PendingSmsResult(4L, 13L))
                every { client.findSendResult(12L) } throws IllegalStateException("연결 실패")
                every { client.findSendResult(13L) } returns UmsSendResultRow(905, 335, "1", "L", null)
                val applied = slot<Map<Long, UmsSendResultRow?>>()
                every { historyService.applyResults(capture(applied)) } returns 1

                SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("실패 건도 조회 시각을 남기고 나머지 건은 계속 갱신된다") {
                    applied.captured.keys shouldContainExactly setOf(3L, 4L)
                    applied.captured[3L].shouldBeNull()
                    applied.captured[4L].shouldNotBeNull().resultCode shouldBe 905
                }
            }

            When("대기 중인 건이 없음") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk(relaxed = true)
                every { historyService.findPending(any(), any()) } returns emptyList()

                val confirmed = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("조회를 시도하지 않는다") {
                    confirmed shouldBe 0
                    verify(exactly = 0) { client.findSendResult(any()) }
                }
            }
        }

        Given("UMS가 응답하지 않는 상황") {
            When("결과 조회가 연속으로 실패") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk()
                every { historyService.findPending(any(), any()) } returns (1L..10L).map { PendingSmsResult(it, it) }
                every { client.findSendResult(any()) } throws IllegalStateException("응답 없음")
                val applied = slot<Map<Long, UmsSendResultRow?>>()
                every { historyService.applyResults(capture(applied)) } returns 0

                SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("남은 건까지 순차 대기하지 않고 이번 주기를 중단한다") {
                    verify(exactly = 3) { client.findSendResult(any()) }
                    applied.captured.keys shouldHaveSize 3
                }
            }
        }

        Given("이력에 결과를 반영") {
            When("정의된 RESULT를 받음") {
                val target = history(clidx = 1L)

                Then("확정으로 표시되고 원본 코드도 남는다") {
                    target.markResultChecked(UmsSendResultRow(903, 333, "0", "S", null)) shouldBe true
                    target.resultCode shouldBe 903
                    target.result shouldBe UmsResultCode.SUCCESS
                    target.statusCode shouldBe 333
                    target.status shouldBe UmsStatusCode.COMPLETED
                    target.isResultConfirmed shouldBe true
                }
            }

            When("정의되지 않은 RESULT를 받음") {
                val target = history(clidx = 1L)

                Then("원본 코드를 남겨 확정으로 처리하고 재조회 대상에서 빠진다") {
                    target.markResultChecked(UmsSendResultRow(907, 999, null, null, null)) shouldBe true
                    target.resultCode shouldBe 907
                    target.result.shouldBeNull()
                    target.statusCode shouldBe 999
                    target.status.shouldBeNull()
                    target.isResultConfirmed shouldBe true
                }
            }

            When("행은 있으나 RESULT가 비어 있음") {
                val target = history(clidx = 1L)

                Then("아직 처리 중으로 보고 다음 주기 대상으로 남는다") {
                    target.markResultChecked(UmsSendResultRow(null, 333, null, "S", null)) shouldBe false
                    target.resultCode.shouldBeNull()
                    target.isResultConfirmed shouldBe false
                    target.resultCheckedAt.shouldNotBeNull()
                }
            }

            When("조회 자체를 못 함") {
                val target = history(clidx = 1L)

                Then("조회 시각만 남긴다") {
                    target.markResultChecked(null) shouldBe false
                    target.resultCode.shouldBeNull()
                    target.resultCheckedAt.shouldNotBeNull()
                }
            }
        }
    })
