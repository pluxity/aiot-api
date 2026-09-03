package com.pluxity.aiot.mic

import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.mic.dto.MicInfo
import com.pluxity.aiot.mic.dto.MicLocation
import com.pluxity.aiot.mic.dto.MicThreshold
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

private fun micInfo(
    id: String = "mic-1",
    name: String? = "Example Mic (715)",
    host: String? = "192.168.0.10",
    edgeId: String? = "edge-1",
    status: String? = "active",
    latitude: Double? = 37.05,
    longitude: Double? = 127.05,
    thresholds: Map<String, MicThreshold>? = mapOf("breathing_heavily" to MicThreshold(65.0, 0.8)),
) = MicInfo(
    id = id,
    name = name,
    host = host,
    edgeId = edgeId,
    status = status,
    thresholds = thresholds,
    location = if (latitude != null && longitude != null) MicLocation(latitude, longitude) else null,
)

class MicServiceKoTest :
    BehaviorSpec({

        Given("AI 마이크 동기화") {
            When("기존에 없던 마이크가 내려옴") {
                val micRepository: MicRepository = mockk(relaxed = true)
                val siteRepository: SiteRepository = mockk()
                val site = SiteFixture.create(id = 1L)
                every { micRepository.findAllWithSite() } returns emptyList()
                every { siteRepository.findFirstByPointInPolygon(any(), any()) } returns site

                val saved = slot<Mic>()
                every { micRepository.save(capture(saved)) } answers { saved.captured }

                MicService(micRepository, siteRepository).sync(listOf(micInfo()))

                Then("마이크가 신규 저장되고 좌표로 현장이 매핑된다") {
                    saved.captured.vendorMicId shouldBe "mic-1"
                    saved.captured.name shouldBe "Example Mic (715)"
                    saved.captured.host shouldBe "192.168.0.10"
                    saved.captured.edgeId shouldBe "edge-1"
                    saved.captured.status shouldBe MicStatus.ACTIVE
                    saved.captured.latitude shouldBe 37.05
                    saved.captured.longitude shouldBe 127.05
                    saved.captured.geom.shouldNotBeNull()
                    saved.captured.site shouldBe site
                    saved.captured.thresholds
                        ?.get("breathing_heavily")
                        ?.soundLevelGe shouldBe 65.0
                }
            }

            When("이미 있는 마이크의 정보가 바뀜") {
                val micRepository: MicRepository = mockk(relaxed = true)
                val siteRepository: SiteRepository = mockk()
                val existing =
                    Mic(vendorMicId = "mic-1", name = "예전 이름", status = MicStatus.ACTIVE).withId(10L)
                every { micRepository.findAllWithSite() } returns listOf(existing)
                every { siteRepository.findFirstByPointInPolygon(any(), any()) } returns null

                MicService(micRepository, siteRepository).sync(listOf(micInfo(name = "새 이름", status = "inactive")))

                Then("기존 엔티티가 갱신되고 신규 저장은 일어나지 않는다") {
                    existing.name shouldBe "새 이름"
                    existing.status shouldBe MicStatus.INACTIVE
                    verify(exactly = 0) { micRepository.save(any()) }
                }
            }

            When("기존 마이크가 업체 목록에서 사라짐") {
                val micRepository: MicRepository = mockk(relaxed = true)
                val siteRepository: SiteRepository = mockk()
                val existing = Mic(vendorMicId = "mic-gone", status = MicStatus.ACTIVE).withId(11L)
                every { micRepository.findAllWithSite() } returns listOf(existing)
                every { siteRepository.findFirstByPointInPolygon(any(), any()) } returns null

                MicService(micRepository, siteRepository).sync(emptyList())

                Then("DISCONNECTED로 표시된다") {
                    existing.status shouldBe MicStatus.DISCONNECTED
                }
            }

            When("좌표가 없는 마이크가 내려옴") {
                val micRepository: MicRepository = mockk(relaxed = true)
                val siteRepository: SiteRepository = mockk()
                every { micRepository.findAllWithSite() } returns emptyList()

                val saved = slot<Mic>()
                every { micRepository.save(capture(saved)) } answers { saved.captured }

                MicService(micRepository, siteRepository)
                    .sync(listOf(micInfo(latitude = null, longitude = null)))

                Then("위치와 현장이 비어 있고 현장 조회를 시도하지 않는다") {
                    saved.captured.latitude shouldBe null
                    saved.captured.geom shouldBe null
                    saved.captured.site shouldBe null
                    verify(exactly = 0) { siteRepository.findFirstByPointInPolygon(any(), any()) }
                }
            }
        }
    })
