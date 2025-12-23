package com.pluxity.aiot.dashboard

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.aiot.action.entity.dummyEventHistoryRow
import com.pluxity.aiot.dashboard.DashboardService.SensorStatisticsRaw
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DashboardServiceKoTest :
    BehaviorSpec({
        val featureRepository: FeatureRepository = mockk()
        val siteRepository: SiteRepository = mockk()
        val eventHistoryRepository: EventHistoryRepository = mockk()

        val dashboardService =
            DashboardService(
                featureRepository = featureRepository,
                siteRepository = siteRepository,
                eventHistoryRepository = eventHistoryRepository,
            )

        Given("센서 요약 조회") {
            When("현장별 통계가 반환되면") {
                val sites = listOf(dummySite(id = 1L, name = "Site A"), dummySite(id = 2L, name = "Site B"))
                every { siteRepository.findAllByOrderByCreatedAtDesc() } returns sites

                val raw1 =
                    createSensorStatisticsRaw(
                        siteId = 1L,
                        siteName = "Site A",
                        totalCount = 10,
                        disconnectedCount = 2,
                        connectedCount = 8,
                        temperatureHumidityCount = 4,
                        fireCount = 3,
                        displacementCount = 1,
                    )
                val raw2 =
                    createSensorStatisticsRaw(
                        siteId = 2L,
                        siteName = "Site B",
                        totalCount = 5,
                        disconnectedCount = 1,
                        connectedCount = 4,
                        temperatureHumidityCount = 2,
                        fireCount = 1,
                        displacementCount = 2,
                    )

                every {
                    featureRepository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<SensorStatisticsRaw>>>(),
                    )
                } returns listOf(raw1, raw2)

                val result = dashboardService.getSensorSummary()

                Then("요약 정보가 매핑된다") {
                    result.size shouldBe 2
                    result[0].siteId shouldBe 1L
                    result[0].siteName shouldBe "Site A"
                    result[0].totalSensors shouldBe 10
                    result[0].connectionStatus.connected shouldBe 8
                    result[0].connectionStatus.disconnected shouldBe 2
                    result[0].sensorTypeStatus.temperatureHumidity shouldBe 4
                    result[0].sensorTypeStatus.fire shouldBe 3
                    result[0].sensorTypeStatus.displacement shouldBe 1
                    result[1].siteId shouldBe 2L
                    result[1].siteName shouldBe "Site B"
                    verify { siteRepository.findAllByOrderByCreatedAtDesc() }
                    verify {
                        featureRepository.findAll(
                            init = any<Jpql.() -> JpqlQueryable<SelectQuery<SensorStatisticsRaw>>>(),
                        )
                    }
                }
            }
        }

        Given("이벤트 요약 조회") {
            When("기간과 현장 필터로 조회하면") {
                val sites = listOf(dummySite(id = 1L, name = "Site A"))
                every { siteRepository.findAllByOrderByCreatedAtDesc() } returns sites

                val activeRow =
                    dummyEventHistoryRow(
                        eventId = 1L,
                        status = EventStatus.ACTIVE,
                        level = ConditionLevel.WARNING,
                        fieldKey = "Temperature",
                    )
                val resolvedRow =
                    dummyEventHistoryRow(
                        eventId = 2L,
                        status = EventStatus.RESOLVED,
                        level = ConditionLevel.CAUTION,
                        fieldKey = "Humidity",
                    )

                every {
                    eventHistoryRepository.findEventList(
                        from = "20240101",
                        to = "20240131",
                        siteIds = listOf(1L),
                    )
                } returns listOf(activeRow, resolvedRow)

                val result = dashboardService.getEventSummary(from = "20240101", to = "20240131")

                Then("상태별로 그룹핑된 결과가 반환된다") {
                    result[EventStatus.ACTIVE]?.size shouldBe 1
                    result[EventStatus.RESOLVED]?.size shouldBe 1
                    result[EventStatus.IN_PROGRESS]?.size shouldBe 0
                    result[EventStatus.ACTIVE]?.first()?.eventId shouldBe 1L
                    result[EventStatus.RESOLVED]?.first()?.eventId shouldBe 2L
                    verify {
                        eventHistoryRepository.findEventList(
                            from = "20240101",
                            to = "20240131",
                            siteIds = listOf(1L),
                        )
                    }
                }
            }
        }
    })

private fun createSensorStatisticsRaw(
    siteId: Long,
    siteName: String,
    totalCount: Long,
    disconnectedCount: Long,
    connectedCount: Long,
    temperatureHumidityCount: Long,
    fireCount: Long,
    displacementCount: Long,
) = SensorStatisticsRaw(
    siteId = siteId,
    siteName = siteName,
    totalCount = totalCount,
    disconnectedCount = disconnectedCount,
    connectedCount = connectedCount,
    temperatureHumidityCount = temperatureHumidityCount,
    fireCount = fireCount,
    displacementCount = displacementCount,
)
