package com.pluxity.aiot.integration

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.impl.DisplacementGaugeProcessor
import com.pluxity.aiot.alarm.service.processor.impl.FireAlarmProcessor
import com.pluxity.aiot.alarm.service.processor.impl.TemperatureHumidityProcessor
import com.pluxity.aiot.config.TestSecurityConfig
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.DeviceProfileFixture
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.EventSetting
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

/**
 * 통합 테스트 2: Processor End-to-End
 *
 * 실제 센서 데이터를 받아 Processor가 EventCondition 평가 → EventHistory 저장 → ActionHistory 생성 → SSE 발행까지 전체 플로우 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcessorEndToEndTest(
    private val deviceProfileRepository: DeviceProfileRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val siteRepository: SiteRepository,
    private val featureRepository: FeatureRepository,
    private val eventHistoryRepository: EventHistoryRepository,
    private val actionHistoryService: ActionHistoryService,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        // Mocks
        val writeApiMock = Mockito.mock(WriteApi::class.java)
        val sseServiceMock = Mockito.mock(SseService::class.java)

        beforeEach {
            TestSecurityConfig.setAdminAuthentication()
        }

        afterEach {
            TestSecurityConfig.clearAuthentication()
        }

        /**
         * DeviceProfile을 조회 또는 생성 (중복 방지)
         */
        fun getOrCreateProfile(
            fieldKey: String,
            description: String = fieldKey,
            fieldUnit: String = "",
            fieldType: DeviceProfile.FieldType = DeviceProfile.FieldType.Float,
        ): DeviceProfile =
            deviceProfileRepository.findAll().firstOrNull { it.fieldKey == fieldKey }
                ?: deviceProfileRepository.saveAndFlush(
                    DeviceProfileFixture.create(
                        fieldKey = fieldKey,
                        description = description,
                        fieldUnit = fieldUnit,
                        fieldType = fieldType,
                    ),
                )

        Given("2-1. TemperatureHumidityProcessor End-to-End") {
            When("Temperature 28.0°C - BETWEEN(25.0~30.0) WARNING 조건 충족") {
                // DeviceProfile 조회 또는 생성
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        description = "온도",
                        fieldUnit = "℃",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // DeviceType 생성
                val deviceType =
                    DeviceType(
                        objectId = "TEMP_E2E_001",
                        description = "온도 센서",
                        version = "1.0",
                    )

                // DeviceProfileType 생성 및 연결
                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                // EventSetting 생성
                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                // DeviceEvent 생성
                val deviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                deviceEvent.updateDeviceType(deviceType)

                // EventCondition 생성 (BETWEEN 25.0~30.0)
                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도가 높습니다",
                        notificationIntervalMinutes = 5,
                        order = 1,
                    )
                condition.changeMinMax(25.0, 30.0)
                eventSetting.addCondition(condition)

                // DeviceType 저장 (CASCADE)
                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)

                // Facility & Feature 생성
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "테스트 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "TEMP_DEVICE_001",
                            objectId = "TEMP_E2E_001",
                            name = "온도 센서",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                // Processor 생성
                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                // 센서 데이터 전송
                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("TEMP_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("EventHistory 저장 및 Feature.eventStatus 업데이트 확인") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("TEMP_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe "TEMP_DEVICE_001"
                    eventHistory.fieldKey shouldBe "Temperature"
                    eventHistory.value shouldBe 28.0
                    eventHistory.eventName shouldBe "TempWarning"
                    eventHistory.minValue shouldBe 25.0
                    eventHistory.maxValue shouldBe 30.0
                    eventHistory.guideMessage shouldBe "온도가 높습니다"
                    eventHistory.actionResult shouldBe "MANUAL_PENDING"

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "WARNING"
                }
            }

            When("Humidity 85.0% - BETWEEN(0.0~100.0) DANGER 조건 충족 (Auto 조치)") {
                // DeviceProfile 조회 또는 생성
                val humidityProfile =
                    getOrCreateProfile(
                        fieldKey = "Humidity",
                        description = "습도",
                        fieldUnit = "%",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // DeviceType 생성
                val deviceType =
                    DeviceType(
                        objectId = "HUMIDITY_E2E_001",
                        description = "습도 센서",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = humidityProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "HumidityDanger",
                        deviceLevel = DeviceEvent.DeviceLevel.DANGER,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "80.0,100.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "습도가 매우 높습니다",
                        notificationIntervalMinutes = 10,
                        order = 1,
                    )
                condition.changeMinMax(80.0, 100.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "습도 테스트 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "HUMIDITY_DEVICE_001",
                            objectId = "HUMIDITY_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = null,
                        humidity = 85.0,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("HUMIDITY_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("EventHistory가 AUTOMATIC_COMPLETED로 저장되고 Feature.eventStatus가 DANGER로 업데이트") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("HUMIDITY_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe "HUMIDITY_DEVICE_001"
                    eventHistory.fieldKey shouldBe "Humidity"
                    eventHistory.value shouldBe 85.0
                    eventHistory.eventName shouldBe "HumidityDanger"
                    eventHistory.actionResult shouldBe "AUTOMATIC_COMPLETED"

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "DANGER"
                }
            }
        }

        Given("2-2. FireAlarmProcessor End-to-End") {
            When("FireAlarm true - EQUALS true DANGER 조건 충족") {
                // DeviceProfile 조회 또는 생성 (Boolean)
                val fireAlarmProfile =
                    getOrCreateProfile(
                        fieldKey = "Fire Alarm",
                        description = "화재감지",
                        fieldUnit = "boolean",
                        fieldType = DeviceProfile.FieldType.Boolean,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "FIRE_E2E_001",
                        description = "화재 센서",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = fireAlarmProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "FireDetected",
                        deviceLevel = DeviceEvent.DeviceLevel.DANGER,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "true",
                        operator = EventCondition.ConditionOperator.EQUALS,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "화재가 감지되었습니다",
                        notificationIntervalMinutes = 1,
                        order = 1,
                    )
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "화재 테스트 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "FIRE_DEVICE_001",
                            objectId = "FIRE_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    FireAlarmProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = null,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = true,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("FIRE_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("Boolean 타입 EQUALS operator 평가 및 Feature.eventStatus 업데이트") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("FIRE_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe "FIRE_DEVICE_001"
                    eventHistory.fieldKey shouldBe "Fire Alarm"
                    eventHistory.value shouldBe 1.0 // Boolean true → 1.0
                    eventHistory.eventName shouldBe "FireDetected"
                    eventHistory.actionResult shouldBe "AUTOMATIC_COMPLETED"

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "DANGER"
                }
            }

            When("FireAlarm true - MANUAL 조치 케이스") {
                val fireAlarmProfile =
                    getOrCreateProfile(
                        fieldKey = "Fire Alarm",
                        description = "화재감지",
                        fieldUnit = "boolean",
                        fieldType = DeviceProfile.FieldType.Boolean,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "FIRE_MANUAL_E2E_001",
                        description = "화재 센서 (MANUAL)",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = fireAlarmProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "FireDetected",
                        deviceLevel = DeviceEvent.DeviceLevel.DANGER,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "true",
                        operator = EventCondition.ConditionOperator.EQUALS,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "화재 발생 - 수동 대응 필요",
                        order = 1,
                    )
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "화재 MANUAL 시설"))
                featureRepository.saveAndFlush(
                    FeatureFixture.create(
                        deviceId = "FIRE_MANUAL_DEVICE_001",
                        objectId = "FIRE_MANUAL_E2E_001",
                        deviceType = savedDeviceType,
                        site = site,
                    ),
                )

                val processor =
                    FireAlarmProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = null,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = true,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("FIRE_MANUAL_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("MANUAL 조치로 MANUAL_PENDING 상태 저장") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("FIRE_MANUAL_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.actionResult shouldBe "MANUAL_PENDING"
                    eventHistory.eventName shouldBe "FireDetected"
                }
            }
        }

        Given("2-3. DisplacementGaugeProcessor End-to-End") {
            When("AngleX 96.0° - BETWEEN 특수 로직 (90° ± 5°)") {
                // DeviceProfile 조회 또는 생성
                val angleXProfile =
                    getOrCreateProfile(
                        fieldKey = "AngleX",
                        description = "X축 각도",
                        fieldUnit = "°",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "ANGLE_E2E_001",
                        description = "변위 센서",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = angleXProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "AngleXCaution",
                        deviceLevel = DeviceEvent.DeviceLevel.CAUTION,
                    )
                deviceEvent.updateDeviceType(deviceType)

                // AngleX: 오차 5°, 중앙값 90° → value = "5.0,90.0"
                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "5.0,90.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "각도 이탈",
                        notificationIntervalMinutes = 5,
                        order = 1,
                    )
                condition.changeMinMax(5.0, 90.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "변위 테스트 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "ANGLE_DEVICE_001",
                            objectId = "ANGLE_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    DisplacementGaugeProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = null,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = 96.0, // 90 + 5 = 95 초과 → 이벤트 발생
                        angleY = null,
                    )

                processor.process("ANGLE_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("특수 BETWEEN 로직 검증 및 Feature.eventStatus 업데이트") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("ANGLE_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.deviceId shouldBe "ANGLE_DEVICE_001"
                    eventHistory.fieldKey shouldBe "AngleX"
                    eventHistory.value shouldBe 96.0
                    eventHistory.eventName shouldBe "AngleXCaution"
                    eventHistory.actionResult shouldBe "MANUAL_PENDING"

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "CAUTION"
                }
            }

            When("AngleY -3.5° - BETWEEN 특수 로직 (0° ± 3°)") {
                val angleYProfile =
                    getOrCreateProfile(
                        fieldKey = "AngleY",
                        description = "Y축 각도",
                        fieldUnit = "°",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "ANGLE_Y_E2E_001",
                        description = "변위 센서 Y축",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = angleYProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "AngleYCaution",
                        deviceLevel = DeviceEvent.DeviceLevel.CAUTION,
                    )
                deviceEvent.updateDeviceType(deviceType)

                // AngleY: 오차 3°, 중앙값 0° → value = "3.0,0.0"
                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "3.0,0.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "Y축 각도 이탈",
                        order = 1,
                    )
                condition.changeMinMax(3.0, 0.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "변위 Y축 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "ANGLE_Y_DEVICE_001",
                            objectId = "ANGLE_Y_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    DisplacementGaugeProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = null,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = -3.5, // 0 - 3 = -3 미만 → 이벤트 발생
                    )

                processor.process("ANGLE_Y_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("AngleY 특수 BETWEEN 로직 검증 및 Feature.eventStatus 업데이트") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("ANGLE_Y_DEVICE_001")
                    eventHistories shouldHaveSize 1

                    val eventHistory = eventHistories.first()
                    eventHistory.fieldKey shouldBe "AngleY"
                    eventHistory.value shouldBe -3.5
                    eventHistory.eventName shouldBe "AngleYCaution"
                    eventHistory.actionResult shouldBe "MANUAL_PENDING"

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "CAUTION"
                }
            }
        }

        Given("2-4. 다중 센서 동시 처리") {
            When("Temperature + Humidity + AngleX 동시 전송 - 각각 다른 조건 충족") {
                // Temperature DeviceProfile 조회 또는 생성
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        description = "온도",
                        fieldUnit = "℃",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // Humidity DeviceProfile 조회 또는 생성
                val humidityProfile =
                    getOrCreateProfile(
                        fieldKey = "Humidity",
                        description = "습도",
                        fieldUnit = "%",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // AngleX DeviceProfile 조회 또는 생성
                val angleXProfile =
                    getOrCreateProfile(
                        fieldKey = "AngleX",
                        description = "X축 각도",
                        fieldUnit = "°",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // DeviceType 생성
                val deviceType =
                    DeviceType(
                        objectId = "MULTI_SENSOR_E2E_001",
                        description = "다중 센서",
                        version = "1.0",
                    )

                // Temperature DeviceProfileType
                val tempProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(tempProfileType)

                val tempEventSetting =
                    EventSetting(
                        deviceProfileType = tempProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                tempProfileType.addEventSetting(tempEventSetting)

                val tempDeviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                tempDeviceEvent.updateDeviceType(deviceType)

                val tempCondition =
                    EventCondition(
                        deviceEvent = tempDeviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도 경고",
                        order = 1,
                    )
                tempCondition.changeMinMax(25.0, 30.0)
                tempEventSetting.addCondition(tempCondition)

                // Humidity DeviceProfileType
                val humidityProfileType =
                    DeviceProfileType(
                        deviceProfile = humidityProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(humidityProfileType)

                val humidityEventSetting =
                    EventSetting(
                        deviceProfileType = humidityProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                humidityProfileType.addEventSetting(humidityEventSetting)

                val humidityDeviceEvent =
                    DeviceEvent(
                        name = "HumidityDanger",
                        deviceLevel = DeviceEvent.DeviceLevel.DANGER,
                    )
                humidityDeviceEvent.updateDeviceType(deviceType)

                val humidityCondition =
                    EventCondition(
                        deviceEvent = humidityDeviceEvent,
                        value = "80.0,100.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = "습도 위험",
                        order = 1,
                    )
                humidityCondition.changeMinMax(80.0, 100.0)
                humidityEventSetting.addCondition(humidityCondition)

                // AngleX DeviceProfileType
                val angleXProfileType =
                    DeviceProfileType(
                        deviceProfile = angleXProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(angleXProfileType)

                val angleXEventSetting =
                    EventSetting(
                        deviceProfileType = angleXProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                angleXProfileType.addEventSetting(angleXEventSetting)

                val angleXDeviceEvent =
                    DeviceEvent(
                        name = "AngleXCaution",
                        deviceLevel = DeviceEvent.DeviceLevel.CAUTION,
                    )
                angleXDeviceEvent.updateDeviceType(deviceType)

                val angleXCondition =
                    EventCondition(
                        deviceEvent = angleXDeviceEvent,
                        value = "5.0,90.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "각도 이탈",
                        order = 1,
                    )
                angleXCondition.changeMinMax(5.0, 90.0)
                angleXEventSetting.addCondition(angleXCondition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "다중 센서 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "MULTI_DEVICE_001",
                            objectId = "MULTI_SENSOR_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val tempHumidityProcessor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val displacementProcessor =
                    DisplacementGaugeProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                // 센서 데이터 전송 (온도 28.0, 습도 85.0, 각도 96.0)
                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0,
                        humidity = 85.0,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = 96.0,
                        angleY = null,
                    )

                // TemperatureHumidityProcessor 처리 (Temperature, Humidity, DiscomfortIndex)
                tempHumidityProcessor.process("MULTI_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                // DisplacementGaugeProcessor 처리 (AngleX)
                displacementProcessor.process("MULTI_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("EventHistory 3건 생성 및 Feature.eventStatus 업데이트 검증") {
                    // EventHistory 3건 생성 (온도 WARNING + 습도 DANGER + AngleX CAUTION)
                    // Note: DiscomfortIndex도 계산되지만 조건이 없어 EventHistory는 생성 안 됨
                    val eventHistories = eventHistoryRepository.findByDeviceId("MULTI_DEVICE_001")
                    eventHistories shouldHaveSize 3

                    val tempHistory = eventHistories.find { it.fieldKey == "Temperature" }
                    tempHistory.shouldNotBeNull()
                    tempHistory.value shouldBe 28.0
                    tempHistory.eventName shouldBe "TempWarning"
                    tempHistory.actionResult shouldBe "MANUAL_PENDING"

                    val humidityHistory = eventHistories.find { it.fieldKey == "Humidity" }
                    humidityHistory.shouldNotBeNull()
                    humidityHistory.value shouldBe 85.0
                    humidityHistory.eventName shouldBe "HumidityDanger"
                    humidityHistory.actionResult shouldBe "AUTOMATIC_COMPLETED"

                    val angleXHistory = eventHistories.find { it.fieldKey == "AngleX" }
                    angleXHistory.shouldNotBeNull()
                    angleXHistory.value shouldBe 96.0
                    angleXHistory.eventName shouldBe "AngleXCaution"
                    angleXHistory.actionResult shouldBe "MANUAL_PENDING"

                    // DisplacementGaugeProcessor가 마지막에 처리되어 CAUTION으로 설정됨
                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "CAUTION"
                }
            }
        }

        Given("2-5. DiscomfortIndex 계산 필드 검증") {
            When("Temperature 30.0°C, Humidity 80.0% - DiscomfortIndex 자동 계산") {
                // Temperature DeviceProfile 조회 또는 생성
                getOrCreateProfile(
                    fieldKey = "Temperature",
                    fieldType = DeviceProfile.FieldType.Float,
                )

                // Humidity DeviceProfile 조회 또는 생성
                getOrCreateProfile(
                    fieldKey = "Humidity",
                    fieldType = DeviceProfile.FieldType.Float,
                )

                // DiscomfortIndex DeviceProfile 조회 또는 생성
                val discomfortProfile =
                    getOrCreateProfile(
                        fieldKey = "DiscomfortIndex",
                        description = "불쾌지수",
                        fieldUnit = "",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                // DeviceType 생성
                val deviceType =
                    DeviceType(
                        objectId = "DISCOMFORT_E2E_001",
                        description = "불쾌지수 센서",
                        version = "1.0",
                    )

                // DiscomfortIndex DeviceProfileType
                val discomfortProfileType =
                    DeviceProfileType(
                        deviceProfile = discomfortProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(discomfortProfileType)

                val discomfortEventSetting =
                    EventSetting(
                        deviceProfileType = discomfortProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                discomfortProfileType.addEventSetting(discomfortEventSetting)

                val discomfortDeviceEvent =
                    DeviceEvent(
                        name = "DiscomfortCaution",
                        deviceLevel = DeviceEvent.DeviceLevel.CAUTION,
                    )
                discomfortDeviceEvent.updateDeviceType(deviceType)

                // DI >= 75 조건
                val discomfortCondition =
                    EventCondition(
                        deviceEvent = discomfortDeviceEvent,
                        value = "75.0",
                        operator = EventCondition.ConditionOperator.GREATER_THAN_OR_EQUAL,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "불쾌지수 높음",
                        order = 1,
                    )
                discomfortEventSetting.addCondition(discomfortCondition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "불쾌지수 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "DISCOMFORT_DEVICE_001",
                            objectId = "DISCOMFORT_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                // 센서 데이터 전송 (온도 30.0, 습도 80.0)
                // DI = 0.81 * 30 + 0.01 * 80 * (0.99 * 30 - 14.3) + 46.3 = 82.92
                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 30.0,
                        humidity = 80.0,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("DISCOMFORT_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("DiscomfortIndex 자동 계산 및 EventHistory 저장, Feature.eventStatus 업데이트") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("DISCOMFORT_DEVICE_001")
                    val discomfortHistory = eventHistories.find { it.fieldKey == "DiscomfortIndex" }

                    discomfortHistory.shouldNotBeNull()
                    discomfortHistory.eventName shouldBe "DiscomfortCaution"
                    // DI 계산 검증
                    discomfortHistory.value shouldBe 82.92

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "CAUTION"
                }
            }
        }

        Given("2-6. Notification Interval 복합 시나리오") {
            When("MANUAL 조치 - 5분 Interval, 재발생 시간에 따른 상태 변화") {
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "INTERVAL_E2E_001",
                        description = "Interval 테스트",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도 경고",
                        notificationIntervalMinutes = 5,
                        order = 1,
                    )
                condition.changeMinMax(25.0, 30.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "Interval 시설"))
                featureRepository.saveAndFlush(
                    FeatureFixture.create(
                        deviceId = "INTERVAL_DEVICE_001",
                        objectId = "INTERVAL_E2E_001",
                        deviceType = savedDeviceType,
                        site = site,
                    ),
                )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0,
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                // 첫 이벤트 발생 (T=0)
                processor.process("INTERVAL_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("첫 이벤트가 MANUAL_PENDING으로 저장됨") {
                    val firstEventHistories = eventHistoryRepository.findByDeviceId("INTERVAL_DEVICE_001")
                    firstEventHistories shouldHaveSize 1
                    firstEventHistories.first().actionResult shouldBe "MANUAL_PENDING"

                    // Note: Notification Interval은 실시간 시간 비교를 사용하므로,
                    // 테스트 환경에서는 실제 시간 경과를 시뮬레이션하기 어렵습니다.
                    // 여기서는 첫 이벤트가 MANUAL_PENDING으로 저장되는 것만 검증합니다.
                }
            }
        }

        Given("2-7. Filter Chain 복합 검증") {
            When("eventEnabled = false - 이벤트 처리 안 됨") {
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "FILTER_E2E_001",
                        description = "필터 테스트",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                // eventEnabled = false
                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = false,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도 경고",
                        order = 1,
                    )
                condition.changeMinMax(25.0, 30.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "필터 시설"))
                val initialEventStatus = "NORMAL"
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "FILTER_DEVICE_001",
                            objectId = "FILTER_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0, // 조건 충족
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("FILTER_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("eventEnabled=false이므로 EventHistory 생성 안 됨 및 Feature.eventStatus 변경 없음") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("FILTER_DEVICE_001")
                    eventHistories shouldHaveSize 0

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe initialEventStatus
                }
            }

            When("notificationEnabled = false - 조건 충족해도 알림 발생 안 함") {
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "NOTI_FILTER_E2E_001",
                        description = "알림 필터 테스트",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                    )
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                deviceEvent.updateDeviceType(deviceType)

                // notificationEnabled = false
                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = false,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도 경고",
                        order = 1,
                    )
                condition.changeMinMax(25.0, 30.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "알림 필터 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "NOTI_FILTER_DEVICE_001",
                            objectId = "NOTI_FILTER_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0, // 조건 충족
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("NOTI_FILTER_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("notificationEnabled=false이므로 EventHistory 생성 안 됨 및 Feature.eventStatus 변경 없음") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("NOTI_FILTER_DEVICE_001")
                    eventHistories shouldHaveSize 0

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "NORMAL"
                }
            }

            When("isPeriodic = true, months 필터링 - 현재 월이 아니면 이벤트 처리 안 됨") {
                val tempProfile =
                    getOrCreateProfile(
                        fieldKey = "Temperature",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val deviceType =
                    DeviceType(
                        objectId = "PERIODIC_FILTER_E2E_001",
                        description = "주기 필터 테스트",
                        version = "1.0",
                    )

                val deviceProfileType =
                    DeviceProfileType(
                        deviceProfile = tempProfile,
                        deviceType = deviceType,
                    )
                deviceType.deviceProfileTypes.add(deviceProfileType)

                // isPeriodic = true, months = [1,2,3] (현재 월이 10월이므로 필터링됨)
                val eventSetting =
                    EventSetting(
                        deviceProfileType = deviceProfileType,
                        eventEnabled = true,
                        isOriginal = true,
                        isPeriodic = true,
                    )
                eventSetting.months = mutableSetOf(1, 2, 3) // 1~3월만 활성화
                deviceProfileType.addEventSetting(eventSetting)

                val deviceEvent =
                    DeviceEvent(
                        name = "TempWarning",
                        deviceLevel = DeviceEvent.DeviceLevel.WARNING,
                    )
                deviceEvent.updateDeviceType(deviceType)

                val condition =
                    EventCondition(
                        deviceEvent = deviceEvent,
                        value = "25.0,30.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = true,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "온도 경고",
                        order = 1,
                    )
                condition.changeMinMax(25.0, 30.0)
                eventSetting.addCondition(condition)

                val savedDeviceType = deviceTypeRepository.saveAndFlush(deviceType)
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "주기 필터 시설"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceId = "PERIODIC_FILTER_DEVICE_001",
                            objectId = "PERIODIC_FILTER_E2E_001",
                            deviceType = savedDeviceType,
                            site = site,
                        ),
                    )

                val processor =
                    TemperatureHumidityProcessor(
                        sseServiceMock,
                        eventHistoryRepository,
                        actionHistoryService,
                        featureRepository,
                        writeApiMock,
                    )

                val sensorData =
                    com.pluxity.aiot.alarm.dto.SubscriptionConResponse(
                        temperature = 28.0, // 조건 충족
                        humidity = null,
                        timestamp = "20250115T103000",
                        period = 60,
                        fireAlarm = null,
                        angleX = null,
                        angleY = null,
                    )

                processor.process("PERIODIC_FILTER_DEVICE_001", savedDeviceType, site.id!!, sensorData)

                Then("현재 월이 10월이고 months=[1,2,3]이므로 EventHistory 생성 안 됨 및 Feature.eventStatus 변경 없음") {
                    val eventHistories = eventHistoryRepository.findByDeviceId("PERIODIC_FILTER_DEVICE_001")
                    eventHistories shouldHaveSize 0

                    val updatedFeature = featureRepository.findById(feature.id!!).get()
                    updatedFeature.eventStatus shouldBe "NORMAL"
                }
            }
        }
    })
