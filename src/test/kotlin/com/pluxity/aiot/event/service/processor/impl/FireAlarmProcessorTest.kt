package com.pluxity.aiot.event.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.entity.HistoryResult
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FireAlarmProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        // Mocks
        val writeApiMock = Mockito.mock(WriteApi::class.java)
        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        // Helper 초기화
        val helper =
            FireAlarmProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApiMock,
                eventConditionRepository,
            )

        Given("화재 감지 센서: EQUALS true 조건") {
            When("fireAlarm = true (1.0) - EQUALS \"true\" 조건 충족") {
                val deviceId = "FIRE_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34956",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                    )

                val sensorData = helper.createSensorData(fireAlarm = true)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("화재 이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Fire Alarm"
                    eventHistories.first().value shouldBe 1.0
                    eventHistories.first().eventName shouldBe "DANGER_Fire Alarm"
                    eventHistories.first().actionResult shouldBe HistoryResult.PENDING
                }
            }

            When("fireAlarm = false (0.0) - EQUALS \"true\" 조건 미충족") {
                val deviceId = "FIRE_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34956",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        isBoolean = true,
                    )

                val sensorData = helper.createSensorData(fireAlarm = false)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("화재 감지 센서: EQUALS false 조건") {
            When("fireAlarm = false (0.0) - EQUALS \"false\" 조건 충족") {
                val deviceId = "FIRE_003"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34956",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "false",
                        maxValue = null,
                        isBoolean = true,
                    )

                val sensorData = helper.createSensorData(fireAlarm = false)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("FireNormal 이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Fire Alarm"
                    eventHistories.first().value shouldBe 0.0
                    eventHistories.first().eventName shouldBe "WARNING_Fire Alarm"
                    eventHistories.first().actionResult shouldBe HistoryResult.PENDING
                }
            }
        }

        // 기존 테스트: "화재 감지 센서: Boolean 부동소수점 오차 테스트"
        // 새로운 구조에서는 Boolean 값을 직접 사용하므로 부동소수점 오차 테스트가 불필요함
        // Boolean은 true/false만 존재하므로 제거됨
    })
