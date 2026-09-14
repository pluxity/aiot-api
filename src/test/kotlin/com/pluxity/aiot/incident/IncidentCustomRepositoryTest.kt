package com.pluxity.aiot.incident

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IncidentCustomRepositoryTest(
    private val incidentRepository: IncidentRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    private val siteRepository: SiteRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        Given("센서·CCTV·MIC incident가 섞여 있을 때") {
            val site = siteRepository.save(SiteFixture.create(name = "통합 현장"))
            val base = LocalDateTime.of(2026, 9, 14, 9, 0)

            val eventHistory =
                eventHistoryRepository.save(
                    EventHistory(
                        deviceId = "THM-1",
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        sensorDescription = "온습도계",
                        fieldKey = "Temperature",
                        value = 31.5,
                        unit = "°C",
                        eventName = "CAUTION_Temperature",
                        occurredAt = base,
                        minValue = 30.0,
                        maxValue = 0.0,
                        level = ConditionLevel.CAUTION,
                    ),
                )
            val sensor =
                incidentRepository.save(
                    Incident(
                        sourceType = IncidentSourceType.SENSOR,
                        sourceId = eventHistory.requiredId,
                        site = site,
                        deviceId = "THM-1",
                        deviceName = "온습도계",
                        title = "온도",
                        level = ConditionLevel.CAUTION,
                        occurredAt = base,
                        latitude = 37.05,
                        longitude = 127.05,
                    ),
                )
            val cctv =
                incidentRepository.save(
                    Incident(
                        sourceType = IncidentSourceType.CCTV,
                        sourceId = 77L,
                        site = site,
                        deviceId = "CAM-1",
                        deviceName = "정문 카메라",
                        title = "배회",
                        level = ConditionLevel.WARNING,
                        occurredAt = base.plusMinutes(10),
                    ),
                )
            val mic =
                incidentRepository.save(
                    Incident(
                        sourceType = IncidentSourceType.MIC,
                        sourceId = 5L,
                        site = null,
                        deviceId = "MIC-1",
                        deviceName = "마이크",
                        title = "비명",
                        level = ConditionLevel.WARNING,
                        occurredAt = base.plusMinutes(20),
                    ).apply { changeStatus(EventStatus.RESOLVED) },
                )

            When("필터 없이 페이징 조회") {
                val rows = incidentRepository.findEventListWithPaging(null, null, size = 10)

                Then("status asc, id desc 순으로 세 건이 나오고 센서 행만 원본 값이 채워진다") {
                    rows shouldHaveSize 3
                    rows.map { it.eventId } shouldBe listOf(cctv.requiredId, sensor.requiredId, mic.requiredId)

                    val sensorRow = rows.first { it.sourceType == IncidentSourceType.SENSOR }
                    sensorRow.objectId shouldBe SensorType.TEMPERATURE_HUMIDITY.objectId
                    sensorRow.fieldKey shouldBe "Temperature"
                    sensorRow.value shouldBe 31.5
                    sensorRow.minValue shouldBe 30.0
                    sensorRow.eventName shouldBe "CAUTION_Temperature"
                    sensorRow.siteId shouldBe site.requiredId
                    sensorRow.siteName shouldBe "통합 현장"

                    val cctvRow = rows.first { it.sourceType == IncidentSourceType.CCTV }
                    cctvRow.objectId shouldBe null
                    cctvRow.fieldKey shouldBe null
                    cctvRow.value shouldBe null
                    cctvRow.title shouldBe "배회"

                    val micRow = rows.first { it.sourceType == IncidentSourceType.MIC }
                    micRow.siteId shouldBe null
                    micRow.status shouldBe EventStatus.RESOLVED
                }
            }

            When("sourceType, sensorType, level, 기간 필터로 조회") {
                val bySource = incidentRepository.findEventListWithPaging(null, null, sourceType = IncidentSourceType.CCTV, size = 10)
                val bySensor =
                    incidentRepository.findEventListWithPaging(
                        null,
                        null,
                        sensorType = SensorType.TEMPERATURE_HUMIDITY,
                        size = 10,
                    )
                val byOtherSensor = incidentRepository.findEventListWithPaging(null, null, sensorType = SensorType.FIRE, size = 10)
                val byLevel = incidentRepository.findEventListWithPaging(null, null, level = ConditionLevel.WARNING, size = 10)
                val byRange = incidentRepository.findEventListWithPaging("20260914090500", "20260914091500", size = 10)
                val bySite = incidentRepository.findEventListWithPaging(null, null, siteId = site.requiredId, size = 10)

                Then("각 필터가 해당 행만 남긴다") {
                    bySource.map { it.eventId } shouldBe listOf(cctv.requiredId)
                    bySensor.map { it.eventId } shouldBe listOf(sensor.requiredId)
                    byOtherSensor shouldHaveSize 0
                    byLevel.map { it.eventId } shouldBe listOf(cctv.requiredId, mic.requiredId)
                    byRange.map { it.eventId } shouldBe listOf(cctv.requiredId)
                    bySite.map { it.eventId } shouldBe listOf(cctv.requiredId, sensor.requiredId)
                }
            }

            When("커서로 두 번째 페이지 조회") {
                val first = incidentRepository.findEventListWithPaging(null, null, size = 1)
                val next = first.last()
                val second =
                    incidentRepository.findEventListWithPaging(
                        null,
                        null,
                        size = 1,
                        lastId = next.eventId,
                        lastStatus = next.status,
                    )

                Then("size+1 건을 돌려주고 커서 이후 행부터 이어진다") {
                    first shouldHaveSize 2
                    second.first().eventId shouldBe next.eventId
                }
            }

            When("대시보드용 전체 조회") {
                val rows = incidentRepository.findEventList(null, null)

                Then("id 내림차순 전체") {
                    rows.map { it.eventId } shouldBe listOf(mic.requiredId, cctv.requiredId, sensor.requiredId)
                }
            }
        }
    })
