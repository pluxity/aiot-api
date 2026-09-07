package com.pluxity.aiot.event.notification

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteSensorManagerService
import com.pluxity.aiot.sms.SmsFacade
import com.pluxity.aiot.sms.SmsValidator
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

private fun event(
    siteName: String = "인천대공원",
    guideMessage: String? = "관리자에게 연락하세요",
    value: Double = 1.0,
) = SensorEventNotified(
    eventId = 7L,
    siteId = 1L,
    siteName = siteName,
    deviceId = "FIRE_001",
    sensorType = SensorType.FIRE,
    level = ConditionLevel.DANGER,
    fieldDescription = "화재감지",
    value = value,
    unit = "",
    guideMessage = guideMessage,
    occurredAt = LocalDateTime.of(2026, 9, 7, 14, 30),
)

class SensorEventSmsListenerKoTest :
    BehaviorSpec({

        Given("알림 대상 이벤트가 발생") {
            When("담당자가 지정되어 있음") {
                val managerService: SiteSensorManagerService = mockk()
                val smsFacade: SmsFacade = mockk()
                every { managerService.findManagerPhoneNumbers(1L, SensorType.FIRE) } returns
                    listOf("010-1111-2222", "010-3333-4444")
                val title = slot<String>()
                val message = slot<String>()
                val targets = slot<List<String>>()
                every { smsFacade.send(capture(title), capture(message), capture(targets)) } returns emptyList()

                SensorEventSmsListener(managerService, smsFacade).onSensorEvent(event())

                Then("담당자 전원에게 이벤트 내용을 보낸다") {
                    targets.captured shouldBe listOf("010-1111-2222", "010-3333-4444")
                    title.captured shouldBe "[인천대공원] 화재감지기"
                    message.captured shouldContain "[DANGER] 화재감지"
                    message.captured shouldContain "FIRE_001"
                    message.captured shouldContain "2026-09-07 14:30"
                    message.captured shouldContain "관리자에게 연락하세요"
                }
            }

            When("담당자가 지정되지 않음") {
                val managerService: SiteSensorManagerService = mockk()
                val smsFacade: SmsFacade = mockk()
                every { managerService.findManagerPhoneNumbers(any(), any()) } returns emptyList()

                SensorEventSmsListener(managerService, smsFacade).onSensorEvent(event())

                Then("발송을 시도하지 않는다") {
                    verify(exactly = 0) { smsFacade.send(any(), any(), any()) }
                }
            }

            When("발송이 예외로 끝남") {
                val managerService: SiteSensorManagerService = mockk()
                val smsFacade: SmsFacade = mockk()
                every { managerService.findManagerPhoneNumbers(any(), any()) } returns listOf("010-1111-2222")
                every { smsFacade.send(any(), any(), any()) } throws IllegalStateException("UMS 장애")

                Then("이벤트 처리까지 실패시키지 않는다") {
                    SensorEventSmsListener(managerService, smsFacade).onSensorEvent(event())
                }
            }

            When("현장명이 길어 제목이 상한을 넘음") {
                val managerService: SiteSensorManagerService = mockk()
                val smsFacade: SmsFacade = mockk()
                every { managerService.findManagerPhoneNumbers(any(), any()) } returns listOf("010-1111-2222")
                val title = slot<String>()
                every { smsFacade.send(capture(title), any(), any()) } returns emptyList()

                SensorEventSmsListener(managerService, smsFacade).onSensorEvent(event(siteName = "가".repeat(60)))

                Then("검증에 걸리지 않도록 잘라서 보낸다") {
                    title.captured.length shouldBe SmsValidator.MAX_TITLE_LENGTH
                }
            }

            When("안내 문구가 없음") {
                val managerService: SiteSensorManagerService = mockk()
                val smsFacade: SmsFacade = mockk()
                every { managerService.findManagerPhoneNumbers(any(), any()) } returns listOf("010-1111-2222")
                val message = slot<String>()
                every { smsFacade.send(any(), capture(message), any()) } returns emptyList()

                SensorEventSmsListener(managerService, smsFacade).onSensorEvent(event(guideMessage = null, value = 27.5))

                Then("빈 줄 없이 끝난다") {
                    message.captured.endsWith("2026-09-07 14:30") shouldBe true
                    message.captured shouldContain "27.5"
                }
            }
        }
    })
