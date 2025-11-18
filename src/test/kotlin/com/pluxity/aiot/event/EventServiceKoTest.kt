package com.pluxity.aiot.event

import com.pluxity.aiot.action.entity.dummyEventHistory
import com.pluxity.aiot.action.entity.dummyEventHistoryRow
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.event.EventService.EventListDto
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.site.entity.dummySite
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

class EventServiceKoTest :
    BehaviorSpec({

        val eventHistoryRepository: EventHistoryRepository = mockk()
        val siteRepository: SiteRepository = mockk()
        val jdbcTemplate: NamedParameterJdbcTemplate = mockk()
        val eventStatusChangeNotifier: EventStatusChangeNotifier = mockk(relaxed = true)

        val eventService =
            EventService(
                eventHistoryRepository,
                siteRepository,
                jdbcTemplate,
                eventStatusChangeNotifier,
            )

        Given("이벤트 목록을 조회할 때") {
            When("모든 파라미터로 조회 요청") {
                val from = "20240101000000"
                val to = "20240131235959"
                val siteId = 1L
                val result = EventStatus.ACTIVE
                val sites =
                    listOf(
                        dummySite(id = 1L),
                        dummySite(id = 2L),
                    )
                val eventHistories =
                    listOf(
                        dummyEventHistoryRow(),
                        dummyEventHistoryRow(),
                    )

                every {
                    siteRepository.findAllByOrderByCreatedAtDesc()
                } returns sites

                every {
                    eventHistoryRepository.findEventListWithPaging(
                        from,
                        to,
                        siteId,
                        result,
                        ConditionLevel.CAUTION,
                        SensorType.DISPLACEMENT_GAUGE,
                        listOf(1L, 2L),
                        20,
                    )
                } returns eventHistories

                Then("이벤트 목록 반환") {
                    val results =
                        eventService.findAll(
                            from,
                            to,
                            siteId,
                            result,
                            ConditionLevel.CAUTION,
                            SensorType.DISPLACEMENT_GAUGE,
                            size = 20,
                        )
                    results.content.size shouldBe 2
                }
            }

            When("필터 없이 조회 요청") {
                val sites = listOf(dummySite(id = 1L))
                val eventHistories = listOf(dummyEventHistoryRow())

                every {
                    siteRepository.findAllByOrderByCreatedAtDesc()
                } returns sites

                every {
                    eventHistoryRepository.findEventListWithPaging(
                        null,
                        null,
                        null,
                        null,
                        ConditionLevel.CAUTION,
                        SensorType.DISPLACEMENT_GAUGE,
                        listOf(1L),
                        20,
                    )
                } returns eventHistories

                Then("전체 이벤트 목록 반환") {
                    val results = eventService.findAll(null, null, null, null, ConditionLevel.CAUTION, SensorType.DISPLACEMENT_GAUGE, 20)
                    results.content.size shouldBe 1
                }
            }
        }

        Given("이벤트 상태를 변경할 때") {
            When("유효한 ID와 상태로 변경 요청") {
                val eventId = 1L
                val newResult = EventStatus.RESOLVED
                val eventHistory = dummyEventHistory(id = eventId)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                Then("상태 변경 성공") {
                    eventService.updateStatus(eventId, newResult)
                    eventHistory.status shouldBe newResult
                }
            }

            When("존재하지 않는 ID로 상태 변경 요청") {
                val eventId = 999L
                val newResult = EventStatus.RESOLVED

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns null

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        eventService.updateStatus(eventId, newResult)
                    }.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }
        }

        Given("이벤트를 ID로 조회할 때") {
            When("유효한 ID로 조회 요청") {
                val eventId = 1L
                val eventHistory = dummyEventHistory(id = eventId)

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns eventHistory

                Then("이벤트 반환") {
                    val result = eventService.findById(eventId)
                    result shouldBe eventHistory
                    result.id shouldBe eventId
                }
            }

            When("존재하지 않는 ID로 조회 요청") {
                val eventId = 999L

                every {
                    eventHistoryRepository.findByIdOrNull(eventId)
                } returns null

                Then("NOT_FOUND_EVENT_HISTORY 예외 발생") {
                    shouldThrowExactly<CustomException> {
                        eventService.findById(eventId)
                    }.message shouldBe ErrorCode.NOT_FOUND_EVENT_HISTORY.getMessage().format(eventId)
                }
            }
        }

        Given("기간별 데이터를 조회할 때") {
            When("시간별 데이터 조회 요청") {
                val interval = DataInterval.HOUR
                val from = "20250101000000"
                val to = "20250101235959"
                val bucket = "10:00"

                every {
                    jdbcTemplate.query(
                        any(),
                        any<Map<String, LocalDateTime>>(),
                        any<RowMapper<EventListDto>>(),
                    )
                } returns
                    listOf(
                        EventListDto(bucket, 1, 2, 3),
                    )

                Then("데이터 반환") {
                    val result = eventService.getPeriodData(interval, from, to)
                    result shouldNotBe null
                    result.timestamps.size shouldBe 1
                    result.metrics.size shouldBe 3
                    result.metrics[EventStatus.ACTIVE.metricKey] shouldNotBe null
                    result.metrics[EventStatus.IN_PROGRESS.metricKey] shouldNotBe null
                    result.metrics[EventStatus.RESOLVED.metricKey] shouldNotBe null
                    result.timestamps[0] shouldBe bucket
                    result.metrics[EventStatus.ACTIVE.metricKey]?.values[0] shouldBe 1
                    result.metrics[EventStatus.IN_PROGRESS.metricKey]?.values[0] shouldBe 2
                    result.metrics[EventStatus.RESOLVED.metricKey]?.values[0] shouldBe 3
                }
            }

            When("일별 데이터 조회 요청") {
                val interval = DataInterval.DAY
                val from = "20250101000000"
                val to = "20250101235959"
                val bucket = "2025-10-01"

                every {
                    jdbcTemplate.query(
                        any(),
                        any<Map<String, LocalDateTime>>(),
                        any<RowMapper<EventListDto>>(),
                    )
                } returns
                    listOf(
                        EventListDto(bucket, 5, 10, 16),
                    )

                Then("일별 시계열 데이터 반환") {
                    val result = eventService.getPeriodData(interval, from, to)
                    result shouldNotBe null
                    result.timestamps.size shouldBe 1
                    result.metrics.size shouldBe 3
                    result.timestamps[0] shouldBe bucket
                    result.metrics[EventStatus.ACTIVE.metricKey] shouldNotBe null
                    result.metrics[EventStatus.IN_PROGRESS.metricKey] shouldNotBe null
                    result.metrics[EventStatus.RESOLVED.metricKey] shouldNotBe null
                    result.metrics[EventStatus.ACTIVE.metricKey]?.values[0] shouldBe 5
                    result.metrics[EventStatus.IN_PROGRESS.metricKey]?.values[0] shouldBe 10
                    result.metrics[EventStatus.RESOLVED.metricKey]?.values[0] shouldBe 16
                }
            }

            When("월별 데이터 조회 요청") {
                val interval = DataInterval.MONTH
                val from = "20240101000000"
                val to = "20250101235959"
                val bucket = "2025-10"

                every {
                    jdbcTemplate.query(
                        any(),
                        any<Map<String, LocalDateTime>>(),
                        any<RowMapper<EventListDto>>(),
                    )
                } returns
                    listOf(
                        EventListDto(bucket, 50, 100, 160),
                    )

                Then("월별 시계열 데이터 반환") {
                    val result = eventService.getPeriodData(interval, from, to)
                    result shouldNotBe null
                    result.timestamps.size shouldBe 1
                    result.metrics.size shouldBe 3
                    result.timestamps[0] shouldBe bucket
                    result.metrics[EventStatus.ACTIVE.metricKey] shouldNotBe null
                    result.metrics[EventStatus.IN_PROGRESS.metricKey] shouldNotBe null
                    result.metrics[EventStatus.RESOLVED.metricKey] shouldNotBe null
                    result.metrics[EventStatus.ACTIVE.metricKey]?.values[0] shouldBe 50
                    result.metrics[EventStatus.IN_PROGRESS.metricKey]?.values[0] shouldBe 100
                    result.metrics[EventStatus.RESOLVED.metricKey]?.values[0] shouldBe 160
                }
            }
        }
    })
