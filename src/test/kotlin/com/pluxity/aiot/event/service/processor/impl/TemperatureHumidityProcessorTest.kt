package com.pluxity.aiot.event.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.SensorType
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
class TemperatureHumidityProcessorTest(
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
            TemperatureHumidityProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApiMock,
                eventConditionRepository,
            )

        Given("온습도계 센서 데이터 처리 및 이벤트 이력 저장") {
            When("온도 28.0°C - BETWEEN(25.0~30.0) Warning 조건 충족") {
                val deviceId = "TH_DEVICE_001"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        guideMessage = "온도가 높습니다",
                    )

                val sensorData = helper.createSensorData(temperature = 28.0)
                val processor = helper.createProcessor()

                // 실행
                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 ACTIVE으로 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe deviceId
                    eventHistory.fieldKey shouldBe "Temperature"
                    eventHistory.value shouldBe 28.0
                    eventHistory.eventName shouldBe "WARNING_Temperature"
                    eventHistory.minValue shouldBe 25.0
                    eventHistory.maxValue shouldBe 30.0
                    eventHistory.status shouldBe EventStatus.ACTIVE
                }
            }

            When("온도 22.0°C - 조건 미충족 (Normal 상태)") {
                val deviceId = "TH_DEVICE_003"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                    )

                val sensorData = helper.createSensorData(temperature = 22.0)
                val processor = helper.createProcessor()

                // 실행
                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장되지 않고 Feature의 eventStatus가 NORMAL이다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val updatedFeature = featureRepository.findByDeviceId(deviceId)
                    updatedFeature.shouldNotBeNull()
                    updatedFeature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("Operator: GREATER_THAN_OR_EQUAL 테스트 (minValue만 사용)") {
            When("온도 30.0°C - GREATER_THAN_OR_EQUAL 25.0 조건 충족") {
                val deviceId = "TH_GT_001"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 30.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 30.0
                    eventHistories.first().eventName shouldBe "WARNING_Temperature"
                }
            }

            When("온도 25.0°C - GREATER_THAN_OR_EQUAL 25.0 조건 충족 (경계값)") {
                val deviceId = "TH_GT_002"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 25.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다 (경계값 포함)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 25.0
                }
            }
        }

        Given("Operator: GREATER_THAN_OR_EQUAL 테스트") {
            When("온도 30.0°C - GREATER_THAN_OR_EQUAL 25.0 조건 충족") {
                val deviceId = "TH_GTE_001"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 30.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 30.0
                }
            }

            When("온도 25.0°C - GREATER_THAN_OR_EQUAL 25.0 조건 충족 (같은 값)") {
                val deviceId = "TH_GTE_002"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 25.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다 (경계값 포함)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 25.0
                }
            }

            When("온도 20.0°C - GREATER_THAN_OR_EQUAL 25.0 조건 미충족") {
                val deviceId = "TH_GTE_003"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 20.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장되지 않는다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0
                }
            }
        }

        Given("Operator: LESS_THAN_OR_EQUAL 테스트 (maxValue만 사용)") {
            When("온도 20.0°C - LESS_THAN_OR_EQUAL 25.0 조건 충족") {
                val deviceId = "TH_LTE_001"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = null,
                        maxValue = "25.0",
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 20.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 20.0
                }
            }

            When("온도 25.0°C - LESS_THAN_OR_EQUAL 25.0 조건 충족 (경계값)") {
                val deviceId = "TH_LTE_002"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = null,
                        maxValue = "25.0",
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 25.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다 (경계값 포함)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 25.0
                }
            }

            When("온도 30.0°C - LESS_THAN_OR_EQUAL 25.0 조건 미충족") {
                val deviceId = "TH_LTE_003"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = null,
                        maxValue = "25.0",
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 30.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장되지 않는다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0
                }
            }
        }

        Given("특수 케이스: 경계값 정확도 테스트") {
            When("BETWEEN 범위의 정확한 minValue (25.0)") {
                val deviceId = "TH_BOUNDARY_001"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        guideMessage = "경계값 테스트",
                    )

                val sensorData = helper.createSensorData(temperature = 25.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다 (경계값 포함)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 25.0
                    eventHistories.first().minValue shouldBe 25.0
                    eventHistories.first().maxValue shouldBe 30.0
                }
            }

            When("BETWEEN 범위의 정확한 maxValue (30.0)") {
                val deviceId = "TH_BOUNDARY_002"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        guideMessage = "경계값 테스트",
                    )

                val sensorData = helper.createSensorData(temperature = 30.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다 (경계값 포함)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 30.0
                    eventHistories.first().minValue shouldBe 25.0
                    eventHistories.first().maxValue shouldBe 30.0
                }
            }
        }

        Given("특수 케이스: 습도 센서 테스트") {
            When("습도 65.0% - BETWEEN(60.0~70.0) 조건 충족") {
                val deviceId = "TH_HUMIDITY_001"
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = "70.0",
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(humidity = 65.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Humidity"
                    eventHistories.first().value shouldBe 65.0
                    eventHistories.first().unit shouldBe "%"
                }
            }
        }

        Given("특수 케이스: 온도와 습도 동시 처리") {
            When("온도 28.0°C와 습도 65.0%가 동시에 조건 충족") {
                val deviceId = "TH_BOTH_001"

                // 온도 조건
                val tempSetup =
                    helper.setupTemperatureDevice(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        guideMessage = "온도 주의",
                    )

                val sensorData = helper.createSensorData(temperature = 28.0, humidity = 65.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(tempSetup.sensorType.objectId), tempSetup.siteId, sensorData)

                Then("온도 이벤트만 저장된다 (습도 조건은 별도 설정 필요)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Temperature"
                    eventHistories.first().value shouldBe 28.0
                }
            }
        }

        Given("특수 케이스: DiscomfortIndex 계산 및 이벤트 처리") {
            When("온도 30.0°C, 습도 70.0% -> DiscomfortIndex 약 78.8 (불쾌지수 위험)") {
                val deviceId = "TH_DISCOMFORT_001"

                // DiscomfortIndex >= 75.0 조건
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "75.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                // 온도 30°C, 습도 70% -> DI = 0.81*30 + 0.01*70*(0.99*30-14.3) + 46.3 ≈ 78.8
                val sensorData = helper.createSensorData(temperature = 30.0, humidity = 70.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("DiscomfortIndex 이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "DiscomfortIndex"
                    // DI = 0.81*30 + 0.01*70*(0.99*30-14.3) + 46.3 = 81.38
                    val actualValue = eventHistories.first().value
                    actualValue.shouldNotBeNull()
                    (actualValue in 81.37..81.39) shouldBe true
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("온도 20.0°C, 습도 50.0% -> DiscomfortIndex 약 63.8 (쾌적)") {
                val deviceId = "TH_DISCOMFORT_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "75.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                // 온도 20°C, 습도 50% -> DI = 0.81*20 + 0.01*50*(0.99*20-14.3) + 46.3 ≈ 63.8
                val sensorData = helper.createSensorData(temperature = 20.0, humidity = 50.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("DiscomfortIndex가 75 미만이므로 이벤트가 발생하지 않고 NORMAL 상태") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("Filter Chain: eventEnabled = false") {
            When("eventEnabled = false인 경우 이벤트가 처리되지 않는다") {
                val deviceId = "TH_DISABLED_001"

                val setup =
                    helper.setupDeviceWithDisabledEvent(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = "30.0",
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(temperature = 28.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("eventEnabled = false이므로 이벤트가 발생하지 않고 NORMAL 상태") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("Filter Chain: notificationEnabled = false") {
            When("notificationEnabled = false인 경우 알림이 발생하지 않는다") {
                val deviceId = "TH_NO_NOTIFICATION_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "25.0",
                        maxValue = "30.0",
                        isBoolean = false,
                    )

                // notificationEnabled를 false로 변경
                val condition = eventConditionRepository.findAllByObjectId(setup.sensorType.objectId).first()
                condition.notificationEnabled = false
                eventConditionRepository.save(condition)

                val sensorData = helper.createSensorData(temperature = 28.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("notificationEnabled = false여도 EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                }
            }
        }

        Given("여러 조건 동시 충족") {
            When("같은 센서에 WARNING과 DANGER 조건이 동시 충족") {
                val deviceId = "TH_MULTI_CONDITION_001"

                val setup =
                    helper.setupDeviceWithMultipleConditions(
                        objectId = "34954",
                        deviceId = deviceId,
                        conditions =
                            listOf(
                                com.pluxity.aiot.event.service.processor.ProcessorTestHelper.ConditionSpec(
                                    eventLevel = ConditionLevel.WARNING,
                                    minValue = "25.0",
                                    maxValue = "30.0",
                                    isBoolean = false,
                                ),
                                com.pluxity.aiot.event.service.processor.ProcessorTestHelper.ConditionSpec(
                                    eventLevel = ConditionLevel.DANGER,
                                    minValue = "28.0",
                                    maxValue = "40.0",
                                    isBoolean = false,
                                ),
                            ),
                    )

                // 28.5°C는 WARNING(25~30)과 DANGER(28~40) 모두 충족
                val sensorData = helper.createSensorData(temperature = 28.5)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("WARNING과 DANGER 이벤트가 모두 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 2
                    eventHistories.map { it.eventName }.toSet() shouldBe setOf("WARNING_Temperature", "DANGER_Temperature")
                    eventHistories.first { it.eventName == "WARNING_Temperature" }.status shouldBe EventStatus.ACTIVE
                    eventHistories.first { it.eventName == "DANGER_Temperature" }.status shouldBe EventStatus.ACTIVE
                }
            }
        }

        Given("Operator: EQUALS 테스트 (Boolean 센서)") {
            When("습도 80.0% - EQUALS 80.0 조건 충족") {
                val deviceId = "TH_EQ_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "80.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(humidity = 80.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().value shouldBe 80.0
                    eventHistories.first().eventName shouldBe "WARNING_Humidity"
                }
            }

            When("습도 75.0% - EQUALS 80.0 조건 미충족") {
                val deviceId = "TH_EQ_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34954",
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "80.0",
                        maxValue = null,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(humidity = 75.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("EventHistory가 저장되지 않는다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0
                }
            }
        }
    })
