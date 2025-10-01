package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.facility.FacilityRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition
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
    deviceTypeRepository: DeviceTypeRepository,
    deviceProfileRepository: DeviceProfileRepository,
    facilityRepository: FacilityRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        // Mocks
        val writeApiMock = Mockito.mock(WriteApi::class.java)
        val sseServiceMock = Mockito.mock(SseService::class.java)

        // Helper 초기화
        val helper =
            TemperatureHumidityProcessorTestHelper(
                deviceTypeRepository,
                deviceProfileRepository,
                facilityRepository,
                featureRepository,
                eventHistoryRepository,
                actionHistoryService,
                sseServiceMock,
                writeApiMock,
            )

        Given("온습도계 센서 데이터 처리 및 이벤트 이력 저장") {
            When("온도 28.0°C - BETWEEN(25.0~30.0) Warning 조건 충족") {
                val deviceId = "TH_DEVICE_001"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "tempHumidity_test1",
                        deviceId = deviceId,
                        eventName = "TempWarning",
                        eventLevel = DeviceEvent.DeviceLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도가 높습니다",
                    )

                val sensorData = helper.createSensorData(temperature = 28.0)
                val processor = helper.createProcessor()

                // 실행
                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("EventHistory가 MANUAL_PENDING으로 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe deviceId
                    eventHistory.fieldKey shouldBe "Temperature"
                    eventHistory.value shouldBe 28.0
                    eventHistory.eventName shouldBe "TempWarning"
                    eventHistory.minValue shouldBe 25.0
                    eventHistory.maxValue shouldBe 30.0
                    eventHistory.guideMessage shouldBe "온도가 높습니다"
                    eventHistory.actionResult shouldBe "MANUAL_PENDING"
                }
            }

            When("온도 35.0°C - BETWEEN(30.0~40.0) Danger 조건 충족 (Auto 조치)") {
                val deviceId = "TH_DEVICE_002"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "tempHumidity_test2",
                        deviceId = deviceId,
                        eventName = "TempDanger",
                        eventLevel = DeviceEvent.DeviceLevel.DANGER,
                        minValue = 30.0,
                        maxValue = 40.0,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "온도가 매우 높습니다",
                    )

                val sensorData = helper.createSensorData(temperature = 35.0)
                val processor = helper.createProcessor()

                // 실행
                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("EventHistory가 AUTOMATIC_COMPLETED로 저장된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe deviceId
                    eventHistory.eventName shouldBe "TempDanger"
                    eventHistory.value shouldBe 35.0
                    eventHistory.minValue shouldBe 30.0
                    eventHistory.maxValue shouldBe 40.0
                    eventHistory.actionResult shouldBe "AUTOMATIC_COMPLETED"
                }
            }

            When("온도 22.0°C - 조건 미충족 (Normal 상태)") {
                val deviceId = "TH_DEVICE_003"
                val setup =
                    helper.setupTemperatureDevice(
                        objectId = "tempHumidity_test3",
                        deviceId = deviceId,
                        eventName = "TempWarning",
                        eventLevel = DeviceEvent.DeviceLevel.WARNING,
                        minValue = 25.0,
                        maxValue = 30.0,
                        controlType = EventCondition.ControlType.MANUAL,
                    )

                val sensorData = helper.createSensorData(temperature = 22.0)
                val processor = helper.createProcessor()

                // 실행
                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("EventHistory가 저장되지 않고 Feature의 eventStatus가 NORMAL이다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val updatedFeature = featureRepository.findByDeviceId(deviceId)
                    updatedFeature.shouldNotBeNull()
                    updatedFeature.eventStatus shouldBe "NORMAL"
                }
            }
        }
    })
