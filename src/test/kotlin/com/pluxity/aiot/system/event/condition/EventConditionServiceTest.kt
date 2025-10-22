package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.system.event.condition.dto.EventConditionRequest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("EventCondition 생성") {
            When("NUMERIC EventCondition 생성") {
                val request =
                    EventConditionRequest(
                        id = null,
                        objectId = "34954",
                        isActivate = true,
                        needControl = false,
                        level = ConditionLevel.WARNING,
                        dataType = DataType.NUMERIC,
                        operator = Operator.GREATER_THAN,
                        numericValue1 = 30.0,
                        numericValue2 = null,
                        booleanValue = null,
                        notificationEnabled = true,
                        notificationIntervalMinutes = 10,
                        order = null,
                    )

                val response = eventConditionService.create(request)

                Then("생성 성공") {
                    response.id shouldNotBe null
                    response.objectId shouldBe "34954"
                    response.level shouldBe ConditionLevel.WARNING
                }
            }
        }
    })
