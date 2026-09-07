package com.pluxity.aiot.sms

import com.pluxity.aiot.global.properties.UmsProperties
import com.pluxity.aiot.sms.dto.SmsSendRequest
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.sql.SQLException

private val umsProperties =
    UmsProperties(
        enabled = true,
        url = "jdbc:sqlserver://db.example.com:11433;databaseName=CIEL_UMS_HOME",
        username = "sync_user",
        systemAccount = "account",
        subCode = "sub",
        senderNumber = "032-000-0000",
    )

private fun request(targetNumber: String = "010-1234-5678") = SmsSendRequest("제목", "내용", targetNumber)

class UmsSmsSenderKoTest :
    BehaviorSpec({

        Given("UMS 호출이 예외로 끝남") {
            When("드라이버 예외에 접속 정보가 담겨 있음") {
                val client: UmsClient = mockk()
                every { client.syncSend(any(), any(), any(), any()) } throws
                    SQLException("The TCP/IP connection to the host db.example.com, port 11433 has failed")

                val result = UmsSmsSender(client, umsProperties).send(request())

                Then("이력에는 호스트·포트를 남기지 않고 호출 실패로 표시한다") {
                    result.stat shouldBe UmsSendStat.NOT_SENT
                    val reason = result.failureReason.shouldNotBeNull()
                    reason shouldBe "UMS 호출 실패: SQLException"
                    reason shouldNotContain "db.example.com"
                    reason shouldNotContain "11433"
                }
            }

            When("예외가 중첩되어 있음") {
                val client: UmsClient = mockk()
                every { client.syncSend(any(), any(), any(), any()) } throws
                    IllegalStateException("래핑", SQLException("호스트 27.101.102.22:11433 접속 실패"))

                val result = UmsSmsSender(client, umsProperties).send(request())

                Then("가장 안쪽 원인의 타입만 남긴다") {
                    result.failureReason shouldBe "UMS 호출 실패: SQLException"
                }
            }

            When("cause가 서로를 가리키는 사이클") {
                val client: UmsClient = mockk()
                val first = IllegalStateException("A")
                val second = IllegalStateException("B", first)
                first.initCause(second)
                every { client.syncSend(any(), any(), any(), any()) } throws second

                val result = UmsSmsSender(client, umsProperties).send(request())

                Then("무한히 순회하지 않고 끝난다") {
                    result.failureReason.shouldNotBeNull() shouldContain "UMS 호출 실패"
                }
            }
        }

        Given("요청이 유효하지 않음") {
            When("수신번호 형식이 잘못됨") {
                val client: UmsClient = mockk()

                val result = UmsSmsSender(client, umsProperties).send(request(targetNumber = "010 1234 5678"))

                Then("호출하지 않고 사유만 남긴다") {
                    verify(exactly = 0) { client.syncSend(any(), any(), any(), any()) }
                    result.failureReason.shouldNotBeNull() shouldContain "수신번호"
                }
            }

            When("발신번호가 설정되지 않음") {
                val client: UmsClient = mockk()

                val result = UmsSmsSender(client, umsProperties.copy(senderNumber = "")).send(request())

                Then("호출하지 않는다") {
                    verify(exactly = 0) { client.syncSend(any(), any(), any(), any()) }
                }
            }
        }

        Given("UMS 연동 설정이 비어 있음") {
            When("필수 설정이 빠진 채로 UmsClient를 만듦") {
                Then("어떤 설정이 빠졌는지 알리며 기동에 실패한다") {
                    val exception =
                        shouldThrowExactly<IllegalArgumentException> {
                            UmsClient(UmsProperties(enabled = true))
                        }
                    val message = exception.message.shouldNotBeNull()
                    message shouldContain "ums.url"
                    message shouldContain "ums.username"
                    message shouldContain "ums.sender-number"
                }
            }

            When("발신번호만 빠짐") {
                Then("발신번호를 지목한다") {
                    val exception =
                        shouldThrowExactly<IllegalArgumentException> {
                            UmsClient(umsProperties.copy(senderNumber = ""))
                        }
                    exception.message.shouldNotBeNull() shouldContain "ums.sender-number"
                }
            }

            When("타임아웃·배치 크기가 0") {
                Then("방어가 사라지는 값이라 기동에 실패한다") {
                    // 0이면 Hikari는 약 24.8일, JDBC 쿼리 타임아웃은 무제한이 된다
                    shouldThrowExactly<IllegalArgumentException> {
                        UmsClient(umsProperties.copy(connectionTimeoutMillis = 0))
                    }.message.shouldNotBeNull() shouldContain "connection-timeout-millis"
                    shouldThrowExactly<IllegalArgumentException> {
                        UmsClient(umsProperties.copy(queryTimeoutSeconds = 0))
                    }.message.shouldNotBeNull() shouldContain "query-timeout-seconds"
                    shouldThrowExactly<IllegalArgumentException> {
                        UmsClient(umsProperties.copy(resultPollBatchSize = 0))
                    }.message.shouldNotBeNull() shouldContain "result-poll-batch-size"
                }
            }
        }
    })
