package com.pluxity.aiot.event

import com.pluxity.aiot.action.entity.dummyIncident
import com.pluxity.aiot.action.entity.dummyIncidentRow
import com.pluxity.aiot.data.enum.DataInterval
import com.pluxity.aiot.event.EventService.EventListDto
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.incident.IncidentRepository
import com.pluxity.aiot.incident.IncidentSourceType
import com.pluxity.aiot.sensor.type.SensorType
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDateTime

class EventServiceKoTest :
    BehaviorSpec({

        val incidentRepository: IncidentRepository = mockk()
        val jdbcTemplate: NamedParameterJdbcTemplate = mockk()
        val eventStatusChangeNotifier: EventStatusChangeNotifier = mockk(relaxed = true)

        val eventService =
            EventService(
                incidentRepository,
                jdbcTemplate,
                eventStatusChangeNotifier,
            )

        Given("이벤트 목록을 조회할 때") {
            When("모든 파라미터로 조회 요청") {
                val from = "20240101000000"
                val to = "20240131235959"
                val siteId = 1L
                val result = EventStatus.ACTIVE
                val rows =
                    listOf(
                        dummyIncidentRow(eventId = 2L),
                        dummyIncidentRow(
                            eventId = 1L,
                            sourceType = IncidentSourceType.CCTV,
                            objectId = null,
                            fieldKey = null,
                            value = null,
                            eventName = null,
                            title = "배회",
                            level = ConditionLevel.WARNING,
                        ),
                    )

                every {
                    incidentRepository.findEventListWithPaging(
                        from,
                        to,
                        siteId,
                        result,
                        ConditionLevel.CAUTION,
                        SensorType.WASTE_FILL_LEVEL,
                        IncidentSourceType.SENSOR,
                        20,
                    )
                } returns rows

                val results =
                    eventService.findAll(
                        from,
                        to,
                        siteId,
                        result,
                        ConditionLevel.CAUTION,
                        SensorType.WASTE_FILL_LEVEL,
                        IncidentSourceType.SENSOR,
                        size = 20,
                    )

                Then("이벤트 목록 반환, 센서가 아닌 행은 title로 eventName을 만든다") {
                    results.content.size shouldBe 2
                    results.content[0].sourceType shouldBe "SENSOR"
                    results.content[0].eventName shouldBe "CAUTION_Temperature"
                    results.content[0].profileDescription shouldBe "온도"
                    results.content[1].sourceType shouldBe "CCTV"
                    results.content[1].eventName shouldBe "WARNING_배회"
                    results.content[1].profileDescription shouldBe "배회"
                    results.content[1].value shouldBe null
                }
            }

            When("필터 없이 조회 요청") {
                every {
                    incidentRepository.findEventListWithPaging(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        20,
                    )
                } returns listOf(dummyIncidentRow())

                val results = eventService.findAll(null, null, null, null, null, null, null, 20)

                Then("전체 이벤트 목록 반환") {
                    results.content.size shouldBe 1
                }
            }
        }

        Given("이벤트 상태를 변경할 때") {
            When("유효한 ID와 상태로 변경 요청") {
                val eventId = 1L
                val newResult = EventStatus.RESOLVED
                val incident = dummyIncident(id = eventId)

                every {
                    incidentRepository.findByIdOrNull(eventId)
                } returns incident

                eventService.updateStatus(eventId, newResult)

                Then("상태 변경 성공, 알림 발송") {
                    incident.status shouldBe newResult
                    verify(exactly = 1) { eventStatusChangeNotifier.notifyStatusChanged(incident) }
                }
            }

            When("존재하지 않는 ID로 상태 변경 요청") {
                val eventId = 999L
                val newResult = EventStatus.RESOLVED

                every {
                    incidentRepository.findByIdOrNull(eventId)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        eventService.updateStatus(eventId, newResult)
                    }

                Then("NOT_FOUND_INCIDENT 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_INCIDENT.getMessage().format(eventId)
                }
            }
        }

        Given("이벤트를 ID로 조회할 때") {
            When("유효한 ID로 조회 요청") {
                val eventId = 1L
                val incident = dummyIncident(id = eventId)

                every {
                    incidentRepository.findByIdOrNull(eventId)
                } returns incident

                val result = eventService.findById(eventId)

                Then("이벤트 반환") {
                    result shouldBe incident
                    result.id shouldBe eventId
                }
            }

            When("존재하지 않는 ID로 조회 요청") {
                val eventId = 999L

                every {
                    incidentRepository.findByIdOrNull(eventId)
                } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        eventService.findById(eventId)
                    }

                Then("NOT_FOUND_INCIDENT 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_INCIDENT.getMessage().format(eventId)
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

                val result = eventService.getPeriodData(interval, from, to)

                Then("데이터 반환") {
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

                val result = eventService.getPeriodData(interval, from, to)

                Then("일별 시계열 데이터 반환") {
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

                val result = eventService.getPeriodData(interval, from, to)

                Then("월별 시계열 데이터 반환") {
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
