package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.UmsSendResultRow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
                val historyService: SmsHistoryService = mockk(relaxed = true)
                val pending = history(clidx = 10L)
                every { historyService.findPending(any()) } returns listOf(pending)
                every { client.findSendResult(10L) } returns
                    UmsSendResultRow(
                        resultCode = 903,
                        statusCode = 333,
                        errorCode = "0",
                        messageType = "S",
                        completedAt = LocalDateTime.of(2026, 9, 3, 10, 0),
                    )

                val updated = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("이력에 결과가 반영된다") {
                    updated shouldBe 1
                    pending.resultCode shouldBe UmsResultCode.SUCCESS
                    pending.statusCode shouldBe UmsStatusCode.COMPLETED
                    pending.errorCode shouldBe "0"
                    pending.messageType shouldBe "S"
                    pending.completedAt shouldBe LocalDateTime.of(2026, 9, 3, 10, 0)
                    pending.isResultConfirmed shouldBe true
                }
            }

            When("아직 결과가 없는 건") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk(relaxed = true)
                val pending = history(clidx = 11L)
                every { historyService.findPending(any()) } returns listOf(pending)
                every { client.findSendResult(11L) } returns null

                val updated = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("결과는 비어 있지만 조회 시각은 남겨 다음 주기에 뒤로 밀린다") {
                    updated shouldBe 0
                    pending.resultCode.shouldBeNull()
                    pending.isResultConfirmed shouldBe false
                    pending.resultCheckedAt.shouldNotBeNull()
                }
            }

            When("한 건 조회가 예외를 던짐") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk(relaxed = true)
                val failing = history(clidx = 12L)
                val succeeding = history(clidx = 13L)
                every { historyService.findPending(any()) } returns listOf(failing, succeeding)
                every { client.findSendResult(12L) } throws IllegalStateException("연결 실패")
                every { client.findSendResult(13L) } returns
                    UmsSendResultRow(905, 335, "1", "L", null)

                val updated = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("나머지 건은 계속 갱신된다") {
                    updated shouldBe 1
                    failing.resultCode.shouldBeNull()
                    // 계속 실패하는 건이 큐 앞을 점유하지 않도록 조회 시각은 남긴다
                    failing.resultCheckedAt.shouldNotBeNull()
                    succeeding.resultCode shouldBe UmsResultCode.FAILURE
                    succeeding.statusCode shouldBe UmsStatusCode.ERROR
                    succeeding.resultCheckedAt.shouldNotBeNull()
                }
            }

            When("대기 중인 건이 없음") {
                val client: UmsClient = mockk()
                val historyService: SmsHistoryService = mockk(relaxed = true)
                every { historyService.findPending(any()) } returns emptyList()

                val updated = SmsResultSyncService(client, historyService, umsProperties).syncPendingResults()

                Then("조회를 시도하지 않는다") {
                    updated shouldBe 0
                    verify(exactly = 0) { client.findSendResult(any()) }
                }
            }
        }
    })
