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
class FireAlarmProcessorTest(
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
            FireAlarmProcessorTestHelper(
                deviceTypeRepository,
                deviceProfileRepository,
                facilityRepository,
                featureRepository,
                eventHistoryRepository,
                actionHistoryService,
                sseServiceMock,
                writeApiMock,
            )

        Given("화재 감지 센서: EQUALS true 조건") {
            When("fireAlarm = true (1.0) - EQUALS \"true\" 조건 충족") {
                val deviceId = "FIRE_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "fire_alarm1",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireDetected",
                        eventLevel = DeviceEvent.DeviceLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "화재가 감지되었습니다",
                        conditionValue = "true",
                    )

                val sensorData = helper.createSensorData(fireAlarm = true)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("화재 이벤트가 저장되고 AUTO로 처리된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Fire Alarm"
                    eventHistories.first().value shouldBe 1.0
                    eventHistories.first().eventName shouldBe "FireDetected"
                    eventHistories.first().actionResult shouldBe "AUTOMATIC_COMPLETED"
                }
            }

            When("fireAlarm = false (0.0) - EQUALS \"true\" 조건 미충족") {
                val deviceId = "FIRE_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "fire_alarm2",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireDetected",
                        eventLevel = DeviceEvent.DeviceLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "화재 확인",
                        conditionValue = "true",
                    )

                val sensorData = helper.createSensorData(fireAlarm = false)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

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
                        objectId = "fire_alarm3",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireNormal",
                        eventLevel = DeviceEvent.DeviceLevel.WARNING,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "화재 센서 정상",
                        conditionValue = "false",
                    )

                val sensorData = helper.createSensorData(fireAlarm = false)
                val processor = helper.createProcessor()

                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("FireNormal 이벤트가 저장되고 MANUAL로 처리된다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Fire Alarm"
                    eventHistories.first().value shouldBe 0.0
                    eventHistories.first().eventName shouldBe "FireNormal"
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }
        }

        Given("화재 감지 센서: NotificationInterval 테스트") {
            When("AUTO 조치 - 5분 내 재발생 시 IGNORED") {
                val deviceId = "FIRE_INTERVAL_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "fire_interval1",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireAlarm",
                        eventLevel = DeviceEvent.DeviceLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "화재 발생",
                        conditionValue = "true",
                        notificationIntervalMinutes = 5,
                    )

                val processor = helper.createProcessor()
                val sensorData = helper.createSensorData(fireAlarm = true)

                // 첫 번째 화재 감지
                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                // 5분 내 두 번째 화재 감지
                processor.process(deviceId, setup.deviceType, setup.facilityId, sensorData)

                Then("첫 번째는 AUTOMATIC_COMPLETED, 두 번째는 AUTOMATIC_IGNORED") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 2
                    eventHistories[0].actionResult shouldBe "AUTOMATIC_COMPLETED"
                    eventHistories[1].actionResult shouldBe "AUTOMATIC_IGNORED"
                }
            }
        }

        Given("화재 감지 센서: Boolean 부동소수점 오차 테스트") {
            When("fireAlarm 값이 0.9995 (거의 1.0) - true로 인식") {
                val deviceId = "FIRE_FLOAT_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "fire_float1",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireDetected",
                        eventLevel = DeviceEvent.DeviceLevel.DANGER,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "화재 감지",
                        conditionValue = "true",
                    )

                // fireAlarm을 직접 설정할 수 없으므로 createSensorData 대신 직접 조건 테스트
                val processor = helper.createProcessor()
                val condition =
                    setup.deviceType.deviceProfileTypes
                        .first()
                        .eventSettings
                        .first()
                        .conditions
                        .first()

                // 0.9995는 1.0과의 차이가 0.001 미만 (0.0005)이므로 true로 인식되어야 함
                val isConditionMet = processor.isConditionMet(condition, 0.9995)

                Then("부동소수점 오차 범위 내에서 true로 인식된다") {
                    isConditionMet shouldBe true
                }
            }

            When("fireAlarm 값이 0.002 (거의 0.0) - false로 인식") {
                val deviceId = "FIRE_FLOAT_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "fire_float2",
                        deviceId = deviceId,
                        profile = helper.fireAlarmProfile,
                        eventName = "FireNormal",
                        eventLevel = DeviceEvent.DeviceLevel.WARNING,
                        minValue = null,
                        maxValue = null,
                        operator = EventCondition.ConditionOperator.EQUALS,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "정상",
                        conditionValue = "false",
                    )

                val processor = helper.createProcessor()
                val condition =
                    setup.deviceType.deviceProfileTypes
                        .first()
                        .eventSettings
                        .first()
                        .conditions
                        .first()

                // 0.002는 0.0과의 차이가 0.001보다 크므로 false로 인식되지 않아야 함
                val isConditionMet = processor.isConditionMet(condition, 0.002)

                Then("부동소수점 오차 범위를 초과하여 false로 인식되지 않는다") {
                    isConditionMet shouldBe false
                }
            }
        }
    })
