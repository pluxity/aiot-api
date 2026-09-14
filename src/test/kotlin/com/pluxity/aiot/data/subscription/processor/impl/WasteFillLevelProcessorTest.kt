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
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import com.pluxity.aiot.incident.IncidentRepository
import com.pluxity.aiot.incident.IncidentService
import com.pluxity.aiot.incident.IncidentSourceType
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
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
    incidentService: IncidentService,
    private val incidentRepository: IncidentRepository,
) : BehaviorSpec({
        extension(SpringExtension(SpringTestLifecycleMode.Root))

        fun newHelper(
            writeApiMock: WriteApi = Mockito.mock(WriteApi::class.java),
            messageSenderMock: StompMessageSender = mockk(relaxed = true),
        ) = WasteFillLevelProcessorTestHelper(
            siteRepository,
            featureRepository,
            eventHistoryRepository,
            messageSenderMock,
            writeApiMock,
            eventConditionRepository,
            incidentService,
        )

        Given("쓰레기 적재 감지기: 단말이 보고한 HighThreshold 기준 만재 판정") {
            When("ActualFilling(20) < HighThreshold(30)") {
                val deviceId = "WFL_001"
                val messageSenderMock = mockk<StompMessageSender>(relaxed = true)
                val helper = newHelper(messageSenderMock = messageSenderMock)
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)

                helper.createProcessor().process(
                    deviceId,
                    setup.sensorType,
                    setup.siteId,
                    helper.createSensorData(containerModuleId = 1, actualFilling = 20, highThreshold = 30),
                )

                Then("WARNING 이벤트가 즉시 저장되고 알림이 전송된다") {
                    val histories = eventHistoryRepository.findByDeviceId(deviceId)
                    histories shouldHaveSize 1
                    val history = histories.first()
                    history.fieldKey shouldBe "ActualFilling"
                    history.value shouldBe 20.0
                    history.unit shouldBe "cm"
                    history.level shouldBe ConditionLevel.WARNING
                    history.eventName shouldBe "WARNING_ActualFilling"
                    history.minValue shouldBe 30.0
                    history.guideMessage shouldBe WasteFillLevelProcessor.FULL_GUIDE_MESSAGE
                    incidentRepository.findBySourceTypeAndSourceId(IncidentSourceType.SENSOR, history.requiredId)?.status shouldBe
                        EventStatus.ACTIVE

                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"

                    val payload = slot<SensorAlarmPayload>()
                    verify(exactly = 1) { messageSenderMock.sendSensorAlarm(capture(payload)) }
                    payload.captured.level shouldBe "WARNING"
                    payload.captured.guideMessage shouldBe WasteFillLevelProcessor.FULL_GUIDE_MESSAGE
                }
            }

            When("ActualFilling(50) >= HighThreshold(30)") {
                val deviceId = "WFL_002"
                val helper = newHelper()
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)

                helper.createProcessor().process(
                    deviceId,
                    setup.sensorType,
                    setup.siteId,
                    helper.createSensorData(containerModuleId = 1, actualFilling = 50, highThreshold = 30),
                )

                Then("이벤트가 발생하지 않고 NORMAL 상태") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }

            When("ActualFilling == HighThreshold") {
                val deviceId = "WFL_003"
                val helper = newHelper()
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)

                helper.createProcessor().process(
                    deviceId,
                    setup.sensorType,
                    setup.siteId,
                    helper.createSensorData(actualFilling = 30, highThreshold = 30),
                )

                Then("경계값은 만재로 보지 않는다") {
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "NORMAL"
                }
            }

            When("만재 후 비워져 ActualFilling이 HighThreshold 이상으로 돌아옴") {
                val deviceId = "WFL_004"
                val helper = newHelper()
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, helper.createSensorData(actualFilling = 10, highThreshold = 30))

                Then("WARNING이 NORMAL로 해제되고 같은 상태의 재수신은 이력을 늘리지 않는다") {
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"

                    processor.process(
                        deviceId,
                        setup.sensorType,
                        setup.siteId,
                        helper.createSensorData(actualFilling = 15, highThreshold = 30),
                    )
                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 1

                    processor.process(
                        deviceId,
                        setup.sensorType,
                        setup.siteId,
                        helper.createSensorData(actualFilling = 80, highThreshold = 30),
                    )
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("쓰레기 적재 감지기: 판단 근거가 부족한 페이로드") {
            When("경보 발생 후 HighThreshold가 빠진 페이로드가 수신됨") {
                val deviceId = "WFL_005"
                val helper = newHelper()
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.sensorType, setup.siteId, helper.createSensorData(actualFilling = 10, highThreshold = 30))

                Then("경보 상태가 유지된다") {
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"

                    processor.process(deviceId, setup.sensorType, setup.siteId, helper.createSensorData(actualFilling = 90))
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"

                    processor.process(
                        deviceId,
                        setup.sensorType,
                        setup.siteId,
                        helper.createSensorData(containerModuleId = 1, highThreshold = 30),
                    )
                    helper.featureRepository.findByDeviceId(deviceId)?.eventStatus shouldBe "WARNING"
                }
            }
        }

        Given("쓰레기 적재 감지기: InfluxDB 적재") {
            When("ContainerModuleId, ActualFilling, HighThreshold가 모두 수신됨") {
                val deviceId = "WFL_006"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = newHelper(writeApiMock = writeApiMock)
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)

                helper.createProcessor().process(
                    deviceId,
                    setup.sensorType,
                    setup.siteId,
                    helper.createSensorData(containerModuleId = 2, actualFilling = 45, highThreshold = 80),
                )

                Then("세 개의 measurement가 fieldKey 태그와 함께 기록된다") {
                    val captor = ArgumentCaptor.forClass(WasteFillLevel::class.java)
                    Mockito
                        .verify(writeApiMock, Mockito.times(3))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), captor.capture())

                    val written = captor.allValues
                    written.map { it.fieldKey } shouldBe listOf("ContainerModuleId", "ActualFilling", "HighThreshold")
                    written.map { it.value } shouldBe listOf(2.0, 45.0, 80.0)
                    written.all { it.deviceId == deviceId } shouldBe true
                    written.all { it.siteId == setup.siteId.toString() } shouldBe true
                }
            }

            When("ActualFilling이 없는 데이터가 수신됨") {
                val deviceId = "WFL_007"
                val writeApiMock = Mockito.mock(WriteApi::class.java)
                val helper = newHelper(writeApiMock = writeApiMock)
                val setup = helper.setupDevice(SensorType.WASTE_FILL_LEVEL.objectId, deviceId)

                helper.createProcessor().process(deviceId, setup.sensorType, setup.siteId, helper.createSensorData(containerModuleId = 3))

                Then("ContainerModuleId만 기록되고 이벤트는 발생하지 않는다") {
                    Mockito
                        .verify(writeApiMock, Mockito.times(1))
                        .writeMeasurement(Mockito.eq(WritePrecision.S), Mockito.any(WasteFillLevel::class.java))

                    eventHistoryRepository.findByDeviceId(deviceId) shouldHaveSize 0
                }
            }
        }

        Given("쓰레기 적재 감지기: 사용자 이벤트 조건 차단") {
            When("ActualFilling으로 이벤트 조건을 등록하려 함") {
                Then("SensorType이 조건 설정을 지원하지 않아 거부된다") {
                    val exception =
                        shouldThrowAny {
                            EventCondition(
                                fieldKey = DeviceProfileEnum.ACTUAL_FILLING.fieldKey,
                                objectId = SensorType.WASTE_FILL_LEVEL.objectId,
                                isActivate = true,
                                level = ConditionLevel.WARNING,
                                conditionType = ConditionType.SINGLE,
                                operator = Operator.LE,
                                thresholdValue = 30.0,
                                notificationEnabled = true,
                            )
                        }
                    exception.message shouldContain "이벤트 조건을 설정할 수 없습니다"
                }
            }

            When("HighThreshold로 이벤트 조건을 등록하려 함") {
                Then("마찬가지로 거부된다") {
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
                }
            }
        }
    })
