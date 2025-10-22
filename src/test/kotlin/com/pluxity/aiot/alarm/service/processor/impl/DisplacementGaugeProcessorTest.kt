package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.EventConditionRepository
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
import com.pluxity.aiot.alarm.type.SensorType

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisplacementGaugeProcessorTest(
    deviceTypeRepository: DeviceTypeRepository,
    deviceProfileRepository: DeviceProfileRepository,
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
    eventConditionRepository: EventConditionRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        // Mocks
        val writeApiMock = Mockito.mock(WriteApi::class.java)
        val sseServiceMock = Mockito.mock(SseService::class.java)

        // Helper 초기화
        val helper =
            DisplacementGaugeProcessorTestHelper(
                deviceTypeRepository,
                deviceProfileRepository,
                siteRepository,
                featureRepository,
                eventHistoryRepository,
                actionHistoryService,
                sseServiceMock,
                writeApiMock,
                eventConditionRepository,
            )

        Given("AngleX: BETWEEN 특수 로직 - 오차범위 처리") {
            When("AngleX = 85.0° - \"5.0,90.0\" (중앙 90°, 오차 ±5°) 범위 밖 (85° <= 85° < 95°)") {
                val deviceId = "DISP_X_001"

                // value는 "오차,중앙값" 형태 -> 실제 범위는 (중앙-오차) ~ (중앙+오차)
                // "5.0,90.0" -> 85° ~ 95° 범위
                // 85° 이하 또는 95° 이상일 때 이벤트 발생
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "WARNING_Angle-X",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "5.0", // 오차
                        maxValue = "90.0", // 중앙값 (AngleX의 기본 중앙값)
                        needControl = false,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 85.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("85°는 85° ~ 95° 범위 경계값 (정확히 minRange)이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-X"
                    eventHistories.first().value shouldBe 85.0
                }
            }

            When("AngleX = 84.0° - \"5.0,90.0\" 범위 밖 (84° < 85°)") {
                val deviceId = "DISP_X_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "DANGER_Angle-X",
                        eventLevel = ConditionLevel.DANGER,
                        minValue = "5.0",
                        maxValue = "90.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 84.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("84°는 범위 밖이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-X"
                    eventHistories.first().value shouldBe 84.0
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }

            When("AngleX = 96.0° - \"5.0,90.0\" 범위 밖 (96° > 95°)") {
                val deviceId = "DISP_X_003"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "AngleXOverRange",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "5.0",
                        maxValue = "90.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 96.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("96°는 범위 밖이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-X"
                    eventHistories.first().value shouldBe 96.0
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }

            When("AngleX = 90.0° - \"5.0,90.0\" 범위 중앙값 (정상)") {
                val deviceId = "DISP_X_004"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "WARNING_Angle-X",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "5.0",
                        maxValue = "90.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 90.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("90°는 범위 중앙값이므로 이벤트가 발생하지 않는다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }
        }

        Given("AngleY: BETWEEN 특수 로직 - 오차범위 처리") {
            When("AngleY = -3.5° - \"3.0,0.0\" (중앙 0°, 오차 ±3°) 범위 밖 (-3.5° < -3°)") {
                val deviceId = "DISP_Y_001"

                // value는 "오차,중앙값" 형태 -> 실제 범위는 (중앙-오차) ~ (중앙+오차)
                // "3.0,0.0" -> -3° ~ 3° 범위
                // -3° 미만 또는 3° 초과일 때 이벤트 발생
                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleYProfile,
                        eventName = "WARNING_Angle-Y",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "3.0", // 오차
                        maxValue = "0.0", // 중앙값 (AngleY의 기본 중앙값)
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleY = -3.5)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("-3.5°는 범위 밖이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-Y"
                    eventHistories.first().value shouldBe -3.5
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }

            When("AngleY = 0.0° - \"3.0,0.0\" 범위 중앙값 (정상)") {
                val deviceId = "DISP_Y_002"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleYProfile,
                        eventName = "WARNING_Angle-Y",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "3.0",
                        maxValue = "0.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleY = 0.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("0°는 범위 중앙값이므로 이벤트가 발생하지 않는다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 0

                    val feature = helper.featureRepository.findByDeviceId(deviceId)
                    feature.shouldNotBeNull()
                    feature.eventStatus shouldBe "NORMAL"
                }
            }

            When("AngleY = 3.5° - \"3.0,0.0\" 범위 밖 (3.5° > 3°)") {
                val deviceId = "DISP_Y_003"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleYProfile,
                        eventName = "DANGER_Angle-Y",
                        eventLevel = ConditionLevel.DANGER,
                        minValue = "3.0",
                        maxValue = "0.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleY = 3.5)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("3.5°는 범위 밖이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-Y"
                    eventHistories.first().value shouldBe 3.5
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }

            When("AngleY = -3.0° - \"3.0,0.0\" 범위 경계값 (정상)") {
                val deviceId = "DISP_Y_004"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleYProfile,
                        eventName = "WARNING_Angle-Y",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "3.0",
                        maxValue = "0.0",
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleY = -3.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("-3°는 범위 경계값 (정확히 minRange)이므로 이벤트가 발생한다") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-Y"
                    eventHistories.first().value shouldBe -3.0
                }
            }
        }

        Given("AngleX/AngleY: 동시 처리") {
            When("AngleX = 95.0°, AngleY = 3.5° - 둘 다 범위 밖") {
                val deviceId = "DISP_BOTH_001"

                // AngleX 조건
                val setupX =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "WARNING_Angle-X",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "5.0",
                        maxValue = "90.0",
                        needControl = false,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 95.0, angleY = 3.5)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setupX.deviceType.objectId), setupX.siteId, sensorData)

                Then("AngleX와 AngleY 모두 이벤트가 발생한다 (objectId 단위 조건 적용)") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 2
                    val angleXEvent = eventHistories.first { it.fieldKey == "Angle-X" }
                    angleXEvent.value shouldBe 95.0
                    val angleYEvent = eventHistories.first { it.fieldKey == "Angle-Y" }
                    angleYEvent.value shouldBe 3.5
                }
            }
        }

        Given("DisplacementGauge: NotificationInterval 테스트") {
            When("MANUAL 조치 - 5분 내 재발생 시 IGNORED") {
                val deviceId = "DISP_INTERVAL_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "WARNING_Angle-X",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "5.0",
                        maxValue = "90.0",
                        needControl = true,
                        isBoolean = false,
                        notificationIntervalMinutes = 5,
                    )

                val processor = helper.createProcessor()

                // 첫 번째 이벤트
                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, helper.createSensorData(angleX = 84.0))

                // 5분 내 두 번째 이벤트
                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, helper.createSensorData(angleX = 83.0))

                Then("첫 번째는 MANUAL_PENDING, 두 번째는 MANUAL_IGNORED") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 2
                    eventHistories[0].actionResult shouldBe "MANUAL_PENDING"
                    eventHistories[1].actionResult shouldBe "MANUAL_IGNORED"
                }
            }
        }

        // 기존 테스트: "AngleX/AngleY: 잘못된 value 형식 (NumberFormatException)"
        // 새로운 구조에서는 numericValue1, numericValue2로 직접 저장되므로 파싱 에러가 발생하지 않음
        // 따라서 이 테스트는 더 이상 의미가 없어 제거됨

        Given("DisplacementGauge: 일반 Operator 테스트 (GREATER_THAN)") {
            When("AngleX = 100.0° - GREATER_THAN 95.0 조건 충족") {
                val deviceId = "DISP_GT_001"

                val setup =
                    helper.setupDeviceWithCondition(
                        objectId = "34957",
                        deviceId = deviceId,
                        profile = helper.angleXProfile,
                        eventName = "AngleXHigh",
                        eventLevel = ConditionLevel.WARNING,
                        minValue = "95.0",
                        maxValue = null,
                        needControl = true,
                        isBoolean = false,
                    )

                val sensorData = helper.createSensorData(angleX = 100.0)
                val processor = helper.createProcessor()

                processor.process(deviceId, SensorType.fromObjectId(setup.deviceType.objectId), setup.siteId, sensorData)

                Then("100° > 95° 조건 충족으로 이벤트 발생") {
                    val eventHistories = eventHistoryRepository.findByDeviceId(deviceId)
                    eventHistories shouldHaveSize 1
                    eventHistories.first().fieldKey shouldBe "Angle-X"
                    eventHistories.first().value shouldBe 100.0
                    eventHistories.first().actionResult shouldBe "MANUAL_PENDING"
                }
            }
        }
    })
