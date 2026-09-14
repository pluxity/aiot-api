package com.pluxity.aiot.incident

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class IncidentServiceKoTest :
    BehaviorSpec({
        val incidentRepository: IncidentRepository = mockk()
        val siteRepository: SiteRepository = mockk()
        val service = IncidentService(incidentRepository, siteRepository)

        val saved = slot<Incident>()
        every { incidentRepository.save(capture(saved)) } answers { saved.captured }

        Given("incident 생성") {
            When("site가 주어지면") {
                val site = dummySite(id = 7L)
                val occurredAt = LocalDateTime.of(2026, 9, 14, 10, 0)

                service.open(
                    sourceType = IncidentSourceType.SENSOR,
                    sourceId = 11L,
                    site = site,
                    deviceId = "DEV-1",
                    deviceName = "온습도계",
                    title = "온도",
                    level = ConditionLevel.CAUTION,
                    occurredAt = occurredAt,
                    latitude = 37.0,
                    longitude = 127.0,
                    guideMessage = "안내",
                )

                Then("폴리곤 조회 없이 그대로 저장되고 상태는 ACTIVE") {
                    verify(exactly = 0) { siteRepository.findFirstByPointInPolygon(any(), any()) }
                    val incident = saved.captured
                    incident.site shouldBe site
                    incident.sourceType shouldBe IncidentSourceType.SENSOR
                    incident.sourceId shouldBe 11L
                    incident.deviceId shouldBe "DEV-1"
                    incident.title shouldBe "온도"
                    incident.level shouldBe ConditionLevel.CAUTION
                    incident.occurredAt shouldBe occurredAt
                    incident.guideMessage shouldBe "안내"
                    incident.status shouldBe EventStatus.ACTIVE
                }
            }

            When("site가 없고 좌표가 있으면") {
                val polygonSite = dummySite(id = 9L)
                every { siteRepository.findFirstByPointInPolygon(127.5, 37.5) } returns polygonSite

                service.open(
                    sourceType = IncidentSourceType.CCTV,
                    sourceId = 3L,
                    site = null,
                    deviceId = "CAM-1",
                    deviceName = null,
                    title = "배회",
                    level = ConditionLevel.WARNING,
                    occurredAt = LocalDateTime.now(),
                    latitude = 37.5,
                    longitude = 127.5,
                )

                Then("좌표 폴리곤으로 site를 보완한다") {
                    saved.captured.site shouldBe polygonSite
                }
            }

            When("site도 좌표도 없으면") {
                service.open(
                    sourceType = IncidentSourceType.MIC,
                    sourceId = 5L,
                    site = null,
                    deviceId = "MIC-1",
                    deviceName = "마이크",
                    title = "비명",
                    level = ConditionLevel.WARNING,
                    occurredAt = LocalDateTime.now(),
                    latitude = null,
                    longitude = null,
                )

                Then("site는 null로 저장된다") {
                    saved.captured.site.shouldBeNull()
                }
            }
        }

        Given("상태 변경") {
            When("RESOLVED로 바꾸면") {
                val incident =
                    Incident(
                        sourceType = IncidentSourceType.SENSOR,
                        sourceId = 1L,
                        deviceId = "DEV",
                        title = "t",
                        level = ConditionLevel.WARNING,
                        occurredAt = LocalDateTime.now(),
                    )
                incident.changeStatus(EventStatus.RESOLVED)

                Then("resolvedAt이 채워지고 다시 IN_PROGRESS로 가면 비워진다") {
                    incident.status shouldBe EventStatus.RESOLVED
                    (incident.resolvedAt != null) shouldBe true
                    incident.changeStatus(EventStatus.IN_PROGRESS)
                    incident.resolvedAt.shouldBeNull()
                }
            }
        }
    })
