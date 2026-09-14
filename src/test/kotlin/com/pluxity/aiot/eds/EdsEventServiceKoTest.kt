package com.pluxity.aiot.eds

import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.eds.dto.EdsEventData
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.file.service.FileService
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.incident.IncidentSourceType
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

private fun eventData(
    id: Int = 100,
    status: Int = EdsEventStatus.STARTED.code,
    type: Int? = EdsEventType.LOITERING.code,
    eventStart: String = "2026/09/14 10:20:30.000",
    latitude: Double? = 37.01,
    longitude: Double? = 127.01,
) = EdsEventData(
    index = 555L,
    id = id,
    profileName = "정문 배회 감지",
    cameraId = "CAM-01",
    type = type,
    eventStart = eventStart,
    status = status,
    latitude = latitude,
    longitude = longitude,
)

class EdsEventServiceKoTest :
    BehaviorSpec({
        val edsEventRepository: EdsEventRepository = mockk(relaxed = true)
        val fileService: FileService = mockk(relaxed = true)
        val cctvRepository: CctvRepository = mockk(relaxed = true)
        val incidentService: IncidentService = mockk(relaxed = true)
        val service = EdsEventService(edsEventRepository, fileService, cctvRepository, incidentService)

        Given("CCTV 이벤트 저장") {
            When("새 이벤트가 시작됨") {
                every { edsEventRepository.findByEventIdAndEventStatusNot(100, EdsEventStatus.ENDED) } returns null
                val saved = slot<EdsEvent>()
                every { edsEventRepository.save(capture(saved)) } answers { saved.captured.withId(9L) }
                val site = dummySite(id = 2L)
                every { cctvRepository.findByEdsCameraId("CAM-01") } returns
                    Cctv(name = "정문 카메라", edsCameraId = "CAM-01", site = site)

                service.saveEvent(eventData(), thumbnailFileId = null)

                Then("incident가 WARNING으로 열리고 eventType 설명이 제목이 된다") {
                    verify(exactly = 1) {
                        incidentService.open(
                            sourceType = IncidentSourceType.CCTV,
                            sourceId = 9L,
                            site = site,
                            deviceId = "CAM-01",
                            deviceName = "정문 카메라",
                            title = "배회",
                            level = ConditionLevel.WARNING,
                            occurredAt = LocalDateTime.of(2026, 9, 14, 10, 20, 30),
                            latitude = 37.01,
                            longitude = 127.01,
                        )
                    }
                }
            }

            When("eventType 코드가 알 수 없고 카메라도 등록되지 않음") {
                every { edsEventRepository.findByEventIdAndEventStatusNot(101, EdsEventStatus.ENDED) } returns null
                val saved = slot<EdsEvent>()
                every { edsEventRepository.save(capture(saved)) } answers { saved.captured.withId(10L) }
                every { cctvRepository.findByEdsCameraId("CAM-01") } returns null

                service.saveEvent(eventData(id = 101, type = 9999, eventStart = "not-a-time"), thumbnailFileId = null)

                Then("profileName이 제목이 되고 카메라 ID가 이름이 되며 시각은 수신 시각으로 대체된다") {
                    val before = LocalDateTime.now().minusSeconds(5)
                    verify(exactly = 1) {
                        incidentService.open(
                            sourceType = IncidentSourceType.CCTV,
                            sourceId = 10L,
                            site = null,
                            deviceId = "CAM-01",
                            deviceName = "CAM-01",
                            title = "정문 배회 감지",
                            level = ConditionLevel.WARNING,
                            occurredAt = match { it >= before },
                            latitude = 37.01,
                            longitude = 127.01,
                        )
                    }
                }
            }

            When("진행 중인 이벤트의 종료 알림이 옴") {
                val existing =
                    EdsEvent(
                        index = 555L,
                        eventId = 100,
                        profileName = "정문 배회 감지",
                        cameraId = "CAM-01",
                        eventStart = "2026/09/14 10:20:30.000",
                        eventStatus = EdsEventStatus.STARTED,
                    ).withId(9L)
                every { edsEventRepository.findByEventIdAndEventStatusNot(100, EdsEventStatus.ENDED) } returns existing
                val incidents: IncidentService = mockk(relaxed = true)
                val endingService = EdsEventService(edsEventRepository, fileService, cctvRepository, incidents)

                endingService.saveEvent(eventData(status = EdsEventStatus.ENDED.code), thumbnailFileId = null)

                Then("원본 상태만 갱신하고 incident는 열지 않는다") {
                    existing.eventStatus shouldBe EdsEventStatus.ENDED
                    verify(exactly = 0) { incidents.open(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
                }
            }
        }
    })
