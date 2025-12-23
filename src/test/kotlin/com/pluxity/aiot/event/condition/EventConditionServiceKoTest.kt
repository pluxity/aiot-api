package com.pluxity.aiot.event.condition

import com.pluxity.aiot.event.condition.dto.EventConditionBatchRequest
import com.pluxity.aiot.event.condition.dto.createDummyEventConditionItemRequest
import com.pluxity.aiot.event.condition.entitiy.dummyEventCondition
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class EventConditionServiceKoTest :
    BehaviorSpec({
        val eventConditionRepository: EventConditionRepository = mockk()
        val eventConditionService = EventConditionService(eventConditionRepository)

        Given("EventCondition 조회") {
            When("objectId로 조회하면") {
                val objectId = SensorType.TEMPERATURE_HUMIDITY.objectId
                val conditions =
                    listOf(
                        dummyEventCondition(
                            id = 1L,
                            objectId = objectId,
                            fieldKey = "Temperature",
                            thresholdValue = 30.0,
                        ),
                    )

                every { eventConditionRepository.findAllByObjectId(objectId) } returns conditions

                val result = eventConditionService.findAllByObjectId(objectId)

                Then("응답으로 매핑된다") {
                    result.size shouldBe 1
                    result.first().id shouldBe 1L
                    result.first().thresholdValue shouldBe 30.0
                    verify { eventConditionRepository.findAllByObjectId(objectId) }
                }
            }

            When("id로 조회하면") {
                val condition =
                    dummyEventCondition(
                        id = 2L,
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        fieldKey = "Temperature",
                        thresholdValue = 25.0,
                    )

                every { eventConditionRepository.findByIdOrNull(2L) } returns condition

                val result = eventConditionService.findById(2L)

                Then("해당 조건이 반환된다") {
                    result.id shouldBe 2L
                    result.thresholdValue shouldBe 25.0
                }
            }

            When("존재하지 않는 id로 조회하면") {
                every { eventConditionRepository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_EVENT_CONDITION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.findById(999L)
                        }
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_EVENT_CONDITION
                }
            }
        }

        Given("EventCondition 생성") {
            When("정상 요청이면") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                createDummyEventConditionItemRequest(
                                    thresholdValue = 30.0,
                                ),
                            ),
                    )
                val savedConditions =
                    listOf(
                        dummyEventCondition(
                            id = 10L,
                            objectId = request.objectId,
                            fieldKey = "Temperature",
                            thresholdValue = 30.0,
                        ),
                    )

                every { eventConditionRepository.saveAll(any<List<EventCondition>>()) } returns savedConditions

                val result = eventConditionService.createBatch(request)

                Then("저장된 id 목록이 반환된다") {
                    result shouldBe listOf(10L)
                    verify { eventConditionRepository.saveAll(any<List<EventCondition>>()) }
                }
            }

            When("범위가 겹치는 조건이 포함되면") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                createDummyEventConditionItemRequest(
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    leftValue = 20.0,
                                    rightValue = 30.0,
                                ),
                                createDummyEventConditionItemRequest(
                                    level = ConditionLevel.CAUTION,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    leftValue = 25.0,
                                    rightValue = 35.0,
                                ),
                            ),
                    )

                Then("DUPLICATE_EVENT_CONDITION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.errorCode shouldBe ErrorCode.DUPLICATE_EVENT_CONDITION
                }
            }
        }

        Given("EventCondition 수정") {
            When("정상 요청이면") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                createDummyEventConditionItemRequest(
                                    level = ConditionLevel.DANGER,
                                    thresholdValue = 35.0,
                                ),
                            ),
                    )
                val savedConditions =
                    listOf(
                        dummyEventCondition(
                            id = 20L,
                            objectId = request.objectId,
                            fieldKey = "Temperature",
                            thresholdValue = 35.0,
                            level = ConditionLevel.DANGER,
                        ),
                    )

                every { eventConditionRepository.deleteAllByObjectId(request.objectId) } returns 1
                every { eventConditionRepository.saveAll(any<List<EventCondition>>()) } returns savedConditions

                val result = eventConditionService.updateBatch(request)

                Then("수정된 응답이 반환된다") {
                    result.size shouldBe 1
                    result.first().level shouldBe ConditionLevel.DANGER
                    result.first().thresholdValue shouldBe 35.0
                    verify { eventConditionRepository.deleteAllByObjectId(request.objectId) }
                    verify { eventConditionRepository.saveAll(any<List<EventCondition>>()) }
                }
            }
        }

        Given("EventCondition 삭제") {
            When("존재하는 조건을 삭제하면") {
                val condition =
                    dummyEventCondition(
                        id = 30L,
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        fieldKey = "Temperature",
                        thresholdValue = 30.0,
                    )

                every { eventConditionRepository.findByIdOrNull(30L) } returns condition
                justRun { eventConditionRepository.delete(condition) }

                eventConditionService.delete(30L)

                Then("삭제가 수행된다") {
                    verify { eventConditionRepository.delete(condition) }
                }
            }

            When("존재하지 않는 조건을 삭제하면") {
                every { eventConditionRepository.findByIdOrNull(999L) } returns null

                Then("NOT_FOUND_EVENT_CONDITION 예외가 발생한다") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.delete(999L)
                        }
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_EVENT_CONDITION
                }
            }
        }

        Given("EventCondition 일괄 삭제") {
            When("objectId로 삭제하면") {
                val objectId = SensorType.TEMPERATURE_HUMIDITY.objectId
                every { eventConditionRepository.deleteAllByObjectId(objectId) } returns 2

                val deletedCount = eventConditionService.deleteAllByObjectId(objectId)

                Then("삭제된 개수가 반환된다") {
                    deletedCount shouldBe 2
                    verify { eventConditionRepository.deleteAllByObjectId(objectId) }
                }
            }
        }
    })
