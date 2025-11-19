package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.condition.EventConditionService
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.event.condition.dto.EventConditionBatchRequest
import com.pluxity.aiot.event.condition.dto.EventConditionItemRequest
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventConditionServiceTest(
    private val eventConditionService: EventConditionService,
    private val eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({

        afterEach {
            eventConditionRepository.deleteAll()
        }

        Given("Valid EventCondition 생성") {
            When("SINGLE type with GOE operator") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {
                    val id =
                        eventConditionService.createBatch(request)[0]
                    val result = eventConditionRepository.findAll().filter { it.id == id }
                    result.size shouldBe 1
                    result[0].thresholdValue shouldBe 30.0
                }
            }

            When("SINGLE type with LOE operator") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Humidity",
                                    level = ConditionLevel.CAUTION,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.LE,
                                    thresholdValue = 20.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 1
                    result[0].thresholdValue shouldBe 20.0
                }
            }

            When("RANGE type with BETWEEN operator") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 25.0,
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 1
                    result[0].leftValue shouldBe 25.0
                    result[0].rightValue shouldBe 35.0
                }
            }

            When("Boolean type condition") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.FIRE.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Fire Alarm",
                                    level = ConditionLevel.DANGER,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = null,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = true,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 1
                    result[0].booleanValue shouldBe true
                }
            }

            When("DisplacementGauge with valid range") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.DISPLACEMENT_GAUGE.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Angle-X",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 5.0, // errorRange
                                    rightValue = 0.0, // centerValue
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 1
                    result[0].leftValue shouldBe 5.0
                    result[0].rightValue shouldBe 0.0
                }
            }
        }

        Given("Invalid objectId") {
            When("objectId가 blank인 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = "",
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "objectId"
                }
            }

            When("존재하지 않는 objectId인 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = "99999",
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "SensorType"
                }
            }
        }

        Given("Invalid fieldKey") {
            When("fieldKey가 blank인 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "  ",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "fieldKey"
                }
            }

            When("해당 objectId에 유효하지 않은 fieldKey인 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "InvalidField",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "fieldKey"
                    exception.message shouldContain "사용할 수 없습니다"
                }
            }
        }

        Given("Boolean type validation") {
            When("booleanValue와 다른 값이 함께 설정된 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.FIRE.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Fire Alarm",
                                    level = ConditionLevel.DANGER,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0, // Boolean type에는 불필요
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = true,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "Boolean"
                }
            }
        }

        Given("SINGLE type validation") {
            When("SINGLE type에 BETWEEN operator 사용") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "SINGLE"
                    exception.message shouldContain "연산자"
                }
            }

            When("SINGLE type에 thresholdValue가 없는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = null,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "SINGLE"
                    exception.message shouldContain "thresholdValue"
                }
            }

            When("SINGLE type에 leftValue나 rightValue가 있는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = 25.0, // SINGLE type에는 불필요
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "SINGLE"
                    exception.message shouldContain "leftValue"
                }
            }
        }

        Given("RANGE type validation") {
            When("RANGE type에 GOE operator 사용") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.GE,
                                    thresholdValue = null,
                                    leftValue = 25.0,
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "RANGE"
                    exception.message shouldContain "BETWEEN"
                }
            }

            When("RANGE type에 leftValue가 없는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = null,
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "RANGE"
                    exception.message shouldContain "leftValue"
                }
            }

            When("RANGE type에 thresholdValue가 있는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = 30.0, // RANGE type에는 불필요
                                    leftValue = 25.0,
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "RANGE"
                    exception.message shouldContain "thresholdValue"
                }
            }
        }

        Given("Range overlap validation") {
            When("같은 objectId, fieldKey의 범위가 겹치는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 20.0,
                                    rightValue = 30.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.CAUTION,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 25.0, // 20~30과 25~35는 겹침
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "조건 범위가 겹칩니다"
                }
            }

            When("DisplacementGauge 범위가 겹치는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.DISPLACEMENT_GAUGE.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Angle-X",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 5.0, // errorRange: 5, center: 0 → -5 ~ 5
                                    rightValue = 0.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                                EventConditionItemRequest(
                                    fieldKey = "Angle-X",
                                    level = ConditionLevel.CAUTION,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 3.0, // errorRange: 3, center: 4 → 1 ~ 7 (겹침)
                                    rightValue = 4.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("CustomException 발생") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventConditionService.createBatch(request)
                        }
                    exception.message shouldContain "조건 범위가 겹칩니다"
                }
            }

            When("범위가 겹치지 않는 경우") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 20.0,
                                    rightValue = 25.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.CAUTION,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 30.0, // 20~25와 30~35는 겹치지 않음
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 2
                }
            }

            When("다른 fieldKey인 경우 범위가 겹쳐도 허용") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 20.0,
                                    rightValue = 30.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                                EventConditionItemRequest(
                                    fieldKey = "Humidity", // 다른 fieldKey
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.RANGE,
                                    operator = Operator.BETWEEN,
                                    thresholdValue = null,
                                    leftValue = 25.0,
                                    rightValue = 35.0,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("정상 생성된다") {

                    eventConditionService.createBatch(request)
                    val result = eventConditionRepository.findAll()
                    result.size shouldBe 2
                }
            }
        }

        Given("CRUD operations") {
            When("조건 생성 후 조회") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )
                val created = eventConditionService.createBatch(request)[0]

                Then("조회가 가능하다") {
                    val found = eventConditionService.findAllByObjectId(SensorType.TEMPERATURE_HUMIDITY.objectId)
                    found.size shouldBe 1
                    found[0].id shouldBe created
                    found[0].thresholdValue shouldBe 30.0
                }
            }

            When("조건 수정") {
                val createRequest =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )
                eventConditionService.createBatch(createRequest)

                val updateRequest =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.DANGER, // WARNING → DANGER
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 35.0, // 30.0 → 35.0
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )

                Then("수정이 가능하다") {
                    val updated = eventConditionService.updateBatch(updateRequest)
                    updated.size shouldBe 1
                    updated[0].level shouldBe ConditionLevel.DANGER
                    updated[0].thresholdValue shouldBe 35.0
                }
            }

            When("조건 삭제") {
                val request =
                    EventConditionBatchRequest(
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        conditions =
                            listOf(
                                EventConditionItemRequest(
                                    fieldKey = "Temperature",
                                    level = ConditionLevel.WARNING,
                                    conditionType = ConditionType.SINGLE,
                                    operator = Operator.GE,
                                    thresholdValue = 30.0,
                                    leftValue = null,
                                    rightValue = null,
                                    booleanValue = null,
                                    activate = true,
                                    notificationEnabled = true,
                                    guideMessage = "안내 문구",
                                ),
                            ),
                    )
                val created = eventConditionService.createBatch(request)[0]

                Then("삭제가 가능하다") {
                    eventConditionService.delete(created)
                    val found = eventConditionService.findAllByObjectId(SensorType.TEMPERATURE_HUMIDITY.objectId)
                    found.size shouldBe 0
                }
            }
        }
    })
