package com.pluxity.aiot.system.event.setting

import com.pluxity.aiot.fixture.DeviceEventFixture
import com.pluxity.aiot.fixture.DeviceProfileFixture
import com.pluxity.aiot.fixture.DeviceProfileTypeFixture
import com.pluxity.aiot.fixture.DeviceTypeFixture
import com.pluxity.aiot.fixture.EventConditionFixture
import com.pluxity.aiot.fixture.EventSettingFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.event.DeviceEventRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileTypeRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.dto.EventConditionRequest
import com.pluxity.aiot.system.event.setting.dto.EventSettingRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventSettingServiceTest(
    private val eventSettingService: EventSettingService,
    private val eventSettingRepository: EventSettingRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val deviceProfileTypeRepository: DeviceProfileTypeRepository,
    private val deviceEventRepository: DeviceEventRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("EventSetting 기본 CRUD") {
            When("ID로 EventSetting을 조회하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_001"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temperature"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )
                val eventSetting = eventSettingRepository.save(EventSettingFixture.create(deviceProfileType = deviceProfileType))

                val result = eventSettingService.getById(eventSetting.id!!)

                Then("해당 EventSetting이 반환된다") {
                    result.shouldNotBeNull()
                    result.id shouldBe eventSetting.id
                }
            }

            When("존재하지 않는 ID로 조회하면") {
                val exception =
                    shouldThrow<CustomException> {
                        eventSettingService.getById(999999L)
                    }

                Then("NOT_FOUND_EVENT_SETTING 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_EVENT_SETTING
                }
            }

            When("전체 EventSetting을 조회하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_ALL"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "humidity"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )

                eventSettingRepository.saveAll(
                    listOf(
                        EventSettingFixture.create(deviceProfileType = deviceProfileType, eventEnabled = true),
                        EventSettingFixture.create(deviceProfileType = deviceProfileType, eventEnabled = false),
                    ),
                )

                val result = eventSettingService.findAll()

                Then("모든 EventSetting이 반환된다") {
                    result.size shouldBe result.size // 기존 데이터 + 새로 추가된 데이터
                }
            }

            When("EventSetting을 삭제하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_DELETE"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "pressure"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )
                val eventSetting = eventSettingRepository.save(EventSettingFixture.create(deviceProfileType = deviceProfileType))

                eventSettingService.delete(eventSetting.id!!)

                Then("EventSetting이 삭제된다") {
                    val exception =
                        shouldThrow<CustomException> {
                            eventSettingService.getById(eventSetting.id!!)
                        }
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_EVENT_SETTING
                }
            }
        }

        Given("EventSetting 업데이트 기능") {
            When("eventEnabled를 변경하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_UPDATE_ENABLED"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temp_enabled"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )
                val deviceEvent = deviceEventRepository.save(DeviceEventFixture.create(name = "TempEventEnabled", deviceType = deviceType))

                val eventSetting =
                    eventSettingRepository.save(
                        EventSettingFixture.create(deviceProfileType = deviceProfileType, eventEnabled = false),
                    )

                val condition = EventConditionFixture.create(deviceEvent = deviceEvent, eventSetting = eventSetting, value = "20.0")
                eventSetting.addCondition(condition)
                eventSettingRepository.save(eventSetting)

                val conditionRequest =
                    EventConditionRequest(
                        id = condition.id,
                        deviceEventId = deviceEvent.id!!,
                        value = "25.0",
                        operator = EventCondition.ConditionOperator.GREATER_THAN,
                        notificationEnabled = true,
                        locationTrackingEnabled = false,
                        soundEnabled = false,
                        fireEffectEnabled = false,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "Test",
                        notificationIntervalMinutes = 10,
                        order = 0,
                    )

                val updateRequest =
                    EventSettingRequest(
                        id = eventSetting.id!!,
                        deviceProfileTypeId = deviceProfileType.id!!,
                        eventEnabled = true,
                        conditions = listOf(conditionRequest),
                        isPeriodic = false,
                        months = null,
                    )

                eventSettingService.updateEventSetting(updateRequest)

                Then("eventEnabled가 변경된다") {
                    val updated = eventSettingService.getById(eventSetting.id!!)
                    updated.eventEnabled shouldBe true
                }
            }

            When("isPeriodic과 months를 설정하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_UPDATE_PERIOD"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temp_period"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )
                val deviceEvent = deviceEventRepository.save(DeviceEventFixture.create(name = "TempEventPeriod", deviceType = deviceType))

                val eventSetting =
                    eventSettingRepository.save(
                        EventSettingFixture.create(deviceProfileType = deviceProfileType, eventEnabled = false),
                    )

                val condition = EventConditionFixture.create(deviceEvent = deviceEvent, eventSetting = eventSetting, value = "20.0")
                eventSetting.addCondition(condition)
                eventSettingRepository.save(eventSetting)

                val conditionRequest =
                    EventConditionRequest(
                        id = condition.id,
                        deviceEventId = deviceEvent.id!!,
                        value = "30.0",
                        operator = EventCondition.ConditionOperator.LESS_THAN,
                        notificationEnabled = false,
                        locationTrackingEnabled = false,
                        soundEnabled = false,
                        fireEffectEnabled = false,
                        controlType = EventCondition.ControlType.AUTO,
                        guideMessage = null,
                        notificationIntervalMinutes = null,
                        order = 0,
                    )

                val updateRequest =
                    EventSettingRequest(
                        id = eventSetting.id!!,
                        deviceProfileTypeId = deviceProfileType.id!!,
                        eventEnabled = false,
                        conditions = listOf(conditionRequest),
                        isPeriodic = true,
                        months = listOf(1, 2, 3),
                    )

                eventSettingService.updateEventSetting(updateRequest)

                Then("isPeriodic과 months가 설정된다") {
                    val updated = eventSettingService.getById(eventSetting.id!!)
                    updated.isPeriodic shouldBe true
                    updated.months?.toList()?.shouldContainAll(listOf(1, 2, 3))
                }
            }

            When("조건의 값을 변경하면 이력이 저장된다") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_UPDATE_HISTORY"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temp_history"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )
                val deviceEvent = deviceEventRepository.save(DeviceEventFixture.create(name = "TempEventHistory", deviceType = deviceType))

                val eventSetting =
                    eventSettingRepository.save(
                        EventSettingFixture.create(deviceProfileType = deviceProfileType, eventEnabled = false),
                    )

                val condition = EventConditionFixture.create(deviceEvent = deviceEvent, eventSetting = eventSetting, value = "20.0")
                eventSetting.addCondition(condition)
                eventSettingRepository.save(eventSetting)

                val conditionRequest =
                    EventConditionRequest(
                        id = condition.id,
                        deviceEventId = deviceEvent.id!!,
                        value = "50.0", // 값 변경
                        operator = EventCondition.ConditionOperator.GREATER_THAN,
                        notificationEnabled = true,
                        locationTrackingEnabled = false,
                        soundEnabled = false,
                        fireEffectEnabled = false,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = "Changed",
                        notificationIntervalMinutes = 20,
                        order = 0,
                    )

                val updateRequest =
                    EventSettingRequest(
                        id = eventSetting.id!!,
                        deviceProfileTypeId = deviceProfileType.id!!,
                        eventEnabled = true,
                        conditions = listOf(conditionRequest),
                        isPeriodic = false,
                        months = null,
                    )

                eventSettingService.updateEventSetting(updateRequest)

                Then("변경 이력이 생성된다") {
                    val histories = eventSettingService.getSettingHistories(eventSetting.id!!)
                    histories shouldHaveSize 1
                }
            }
        }

        Given("조건 범위 중복 검증") {
            When("BETWEEN 조건의 범위가 중복되면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_OVERLAP"))
                val deviceProfile = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temp_overlap"))
                val deviceProfileType =
                    deviceProfileTypeRepository.save(
                        DeviceProfileTypeFixture.create(deviceType = deviceType, deviceProfile = deviceProfile),
                    )

                val deviceEvent1 = deviceEventRepository.save(DeviceEventFixture.create(name = "Event1", deviceType = deviceType))
                val deviceEvent2 = deviceEventRepository.save(DeviceEventFixture.create(name = "Event2", deviceType = deviceType))

                val eventSetting = eventSettingRepository.save(EventSettingFixture.create(deviceProfileType = deviceProfileType))

                val condition1 =
                    EventConditionFixture.create(
                        deviceEvent = deviceEvent1,
                        eventSetting = eventSetting,
                        value = "10.0,20.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                    )
                val condition2 =
                    EventConditionFixture.create(
                        deviceEvent = deviceEvent2,
                        eventSetting = eventSetting,
                        value = "15.0,25.0",
                        operator = EventCondition.ConditionOperator.BETWEEN,
                    )

                eventSetting.addCondition(condition1)
                eventSetting.addCondition(condition2)
                eventSettingRepository.save(eventSetting)

                val conditionRequest1 =
                    EventConditionRequest(
                        id = condition1.id,
                        deviceEventId = deviceEvent1.id!!,
                        value = "10.0,20.0", // 범위: 10~20
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = false,
                        locationTrackingEnabled = false,
                        soundEnabled = false,
                        fireEffectEnabled = false,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = null,
                        notificationIntervalMinutes = null,
                        order = 0,
                    )

                val conditionRequest2 =
                    EventConditionRequest(
                        id = condition2.id,
                        deviceEventId = deviceEvent2.id!!,
                        value = "15.0,25.0", // 범위: 15~25 (중복!)
                        operator = EventCondition.ConditionOperator.BETWEEN,
                        notificationEnabled = false,
                        locationTrackingEnabled = false,
                        soundEnabled = false,
                        fireEffectEnabled = false,
                        controlType = EventCondition.ControlType.MANUAL,
                        guideMessage = null,
                        notificationIntervalMinutes = null,
                        order = 1,
                    )

                val updateRequest =
                    EventSettingRequest(
                        id = eventSetting.id!!,
                        deviceProfileTypeId = deviceProfileType.id!!,
                        eventEnabled = true,
                        conditions = listOf(conditionRequest1, conditionRequest2),
                        isPeriodic = false,
                        months = null,
                    )

                val exception =
                    shouldThrow<CustomException> {
                        eventSettingService.updateEventSetting(updateRequest)
                    }

                Then("DUPLICATE_EVENT_CONDITION 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.DUPLICATE_EVENT_CONDITION
                }
            }
        }
    })
