package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.pluxity.aiot.data.measure.WasteFillLevel
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.SpringTestLifecycleMode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WasteFillLevelProcessorTest(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        // Mocks
        val writeApiMock = Mockito.mock(WriteApi::class.java)
        val messageSenderMock = Mockito.mock(StompMessageSender::class.java)

        // Helper 초기화
        val helper =
            WasteFillLevelProcessorTestHelper(
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                messageSenderMock,
                writeApiMock,
                eventConditionRepository,
            )

        Given("쓰레기 적재 감지기: ActualFilling 만재 조건(GE 60cm)") {
            When("ActualFilling = 70cm - 조건 충족") {
                val deviceId = "WFL_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val sensorData = helper.createSensorData(containerModuleId = 1, actualFilling = 70)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("만재 이벤트가 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "ActualFilling"
                    eventHistories.first().value shouldBe 70.0
                    eventHistories.first().unit shouldBe "cm"
                    eventHistories.first().eventName shouldBe "WARNING_ActualFilling"
                    eventHistories.first().status shouldBe EventStatus.ACTIVE
                }
            }

            When("ActualFilling = 30cm - 조건 미충족") {
                val deviceId = "WFL_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val sensorData = helper.createSensorData(containerModuleId = 1, actualFilling = 30)
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

        Given("쓰레기 적재 감지기: InfluxDB 적재") {
            When("ContainerModuleId, ActualFilling, HighThreshold가 모두 수신됨") {
                val deviceId = "WFL_003"
                val localWriteApiMock = Mockito.mock(WriteApi::class.java)
                val localHelper =
                    WasteFillLevelProcessorTestHelper(
                        siteRepository,
                        featureRepository,
                        eventHistoryRepository,
                        messageSenderMock,
                        localWriteApiMock,
                        eventConditionRepository,
                    )

                val setup =
                    localHelper.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val sensorData =
                    localHelper.createSensorData(
                        containerModuleId = 2,
                        actualFilling = 45,
                        highThreshold = 80,
                    )
                localHelper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("세 개의 measurement가 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(WasteFillLevel::class.java)
                    Mockito
                        .verify(localWriteApiMock, Mockito.times(3))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe listOf("ContainerModuleId", "ActualFilling", "HighThreshold")
                    written.map { it.value } shouldBe listOf(2.0, 45.0, 80.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("ActualFilling이 없는 데이터가 수신됨") {
                val deviceId = "WFL_004"
                val localWriteApiMock = Mockito.mock(WriteApi::class.java)
                val localHelper =
                    WasteFillLevelProcessorTestHelper(
                        siteRepository,
                        featureRepository,
                        eventHistoryRepository,
                        messageSenderMock,
                        localWriteApiMock,
                        eventConditionRepository,
                    )

                val setup =
                    localHelper.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val sensorData = localHelper.createSensorData(containerModuleId = 3)
                localHelper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("ContainerModuleId만 기록되고 이벤트는 발생하지 않는다") {
                    Mockito
                        .verify(localWriteApiMock, Mockito.times(1))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(WasteFillLevel::class.java))

                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0
                }
            }
        }

        Given("쓰레기 적재 감지기: 이벤트 조건 대상이 아닌 항목") {
            When("HighThreshold로 이벤트 조건을 등록하려 함") {
                Then("SensorType의 프로필에 없어 저장이 거부된다") {
                    val exception =
                        shouldThrowAny {
                            EventCondition(
                                fieldKey = DeviceProfileEnum.HIGH_THRESHOLD.fieldKey,
                                objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                                isActivate = true,
                                level = ConditionLevel.WARNING,
                                conditionType = ConditionType.SINGLE,
                                operator = Operator.GE,
                                thresholdValue = 60.0,
                                notificationEnabled = true,
                            )
                        }
                    exception.message shouldContain "HighThreshold"
                }
            }

            When("ContainerModuleId로 이벤트 조건을 등록하려 함") {
                Then("마찬가지로 저장이 거부된다") {
                    shouldThrowAny {
                        EventCondition(
                            fieldKey = DeviceProfileEnum.CONTAINER_MODULE_ID.fieldKey,
                            objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                            isActivate = true,
                            level = ConditionLevel.WARNING,
                            conditionType = ConditionType.SINGLE,
                            operator = Operator.GE,
                            thresholdValue = 1.0,
                            notificationEnabled = true,
                        )
                    }
                }
            }

            When("HighThreshold가 담긴 데이터가 수신됨") {
                val deviceId = "WFL_005"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val localHelper =
                    WasteFillLevelProcessorTestHelper(
                        siteRepository,
                        featureRepository,
                        eventHistoryRepository,
                        messageSenderMock,
                        writeApiMock,
                        eventConditionRepository,
                    )

                val setup =
                    localHelper.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val sensorData = localHelper.createSensorData(actualFilling = 30, highThreshold = 80)
                localHelper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, sensorData)

                Then("이벤트는 발생하지 않지만 값은 InfluxDB에 적재된다") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val captor = ArgumentCaptor.forClass(WasteFillLevel::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(2))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())
                    captor.allValues.map { it.fieldKey } shouldBe listOf("ActualFilling", "HighThreshold")
                }
            }
        }

        Given("쓰레기 적재 감지기: 조건 대상 값이 없는 페이로드") {
            When("경보 발생 후 ActualFilling이 빠진 페이로드가 수신됨") {
                val deviceId = "WFL_006"
                val helper2 =
                    WasteFillLevelProcessorTestHelper(
                        siteRepository,
                        featureRepository,
                        eventHistoryRepository,
                        messageSenderMock,
                        Mockito.mock(WriteApi::class.java),
                        eventConditionRepository,
                    )

                val setup =
                    helper2.setupDeviceWithCondition(
                        objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                        deviceId = deviceId,
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "60.0",
                        maxValue = null,
                        isBoolean = false,
                        fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                    )

                val processor = helper2.createProcessor()
                processor.process(
                    deviceId,
                    setup.sensorType,
                    setup.siteId,
                    helper2.createSensorData(actualFilling = 70),
                )

                // ProjectConfig가 InstancePerLeaf라 Then이 여러 개면 When 본문이 재실행된다.
                // Feature를 insert하는 컨테이너이므로 검증은 하나의 Then에 모은다
                Then("판단 근거가 없으므로 경보 상태가 유지된다") {
                    helper2.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"

                    processor.process(
                        deviceId,
                        setup.sensorType,
                        setup.siteId,
                        helper2.createSensorData(containerModuleId = 1, highThreshold = 80),
                    )

                    val feature = helper2.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "WARNING"
                }
            }
        }
    })
