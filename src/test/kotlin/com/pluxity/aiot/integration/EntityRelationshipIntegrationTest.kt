package com.pluxity.aiot.integration

import com.pluxity.aiot.config.TestSecurityConfig
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.DeviceProfileFixture
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.event.DeviceEventRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.device.type.DeviceTypeService
import com.pluxity.aiot.system.device.type.dto.DeviceEventRequest
import com.pluxity.aiot.system.device.type.dto.DeviceProfileTypeRequest
import com.pluxity.aiot.system.device.type.dto.DeviceTypeRequest
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.setting.EventSettingRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 테스트 1: 엔티티 연관관계 통합
 *
 * 전체 엔티티가 복합적으로 연결된 환경에서 CRUD 작업이 정상 동작하는지 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityRelationshipIntegrationTest(
    private val deviceProfileRepository: DeviceProfileRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceTypeService: DeviceTypeService,
    private val deviceEventRepository: DeviceEventRepository,
    private val eventSettingRepository: EventSettingRepository,
    private val siteRepository: SiteRepository,
    private val featureRepository: FeatureRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        beforeEach {
            TestSecurityConfig.setAdminAuthentication()
        }

        afterEach {
            TestSecurityConfig.clearAuthentication()
        }

        Given("1-1. DeviceProfile → DeviceType → EventSetting → EventCondition 연쇄 생성") {
            When("Float 타입 DeviceProfile과 DeviceType을 함께 생성하면") {
                // 기존 DeviceTypeServiceTest 로직 재사용
                val profile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_int_1"))
                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_INT_001",
                        description = "Integration Sensor",
                        version = "1.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "Warning", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id!!, 0.0, 100.0)),
                    )
                val createdId = deviceTypeService.create(request)
                val saved = deviceTypeRepository.findByIdWithAssociations(createdId)!!

                Then("DeviceProfileType, EventSetting, EventCondition이 cascade 생성된다") {
                    saved.deviceProfileTypes shouldHaveSize 1
                    saved.deviceEvents shouldHaveSize 2

                    val profileType = saved.deviceProfileTypes.first()
                    val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!)
                    eventSettings shouldHaveSize 1

                    val eventSetting = eventSettings.first()
                    eventSetting.isOriginal shouldBe true
                    eventSetting.conditions shouldHaveSize 2 // 각 DeviceEvent마다

                    eventSetting.conditions.forEach { condition ->
                        condition.operator shouldBe EventCondition.ConditionOperator.BETWEEN
                        condition.minValue shouldBe 0.0
                        condition.maxValue shouldBe 100.0
                    }
                }
            }

            When("Boolean 타입 DeviceProfile로 생성하면") {
                val profile =
                    deviceProfileRepository.saveAndFlush(
                        DeviceProfileFixture.create(fieldKey = "fire_int_1", fieldType = com.pluxity.aiot.system.device.profile.DeviceProfile.FieldType.Boolean),
                    )
                val request =
                    DeviceTypeRequest(
                        objectId = "FIRE_INT_001",
                        description = "Fire Alarm",
                        version = "1.0",
                        deviceEvents = listOf(DeviceEventRequest(null, "FireDetected", DeviceEvent.DeviceLevel.DANGER, null)),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id!!, null, null)),
                    )
                val createdId = deviceTypeService.create(request)
                val saved = deviceTypeRepository.findByIdWithAssociations(createdId)!!

                Then("EventCondition operator가 EQUALS로 설정된다") {
                    val profileType = saved.deviceProfileTypes.first()
                    val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!)
                    val condition = eventSettings.first().conditions.first()

                    condition.operator shouldBe EventCondition.ConditionOperator.EQUALS
                    condition.minValue.shouldBeNull()
                    condition.maxValue.shouldBeNull()
                }
            }

            When("여러 DeviceProfile을 동시에 연결하면") {
                val tempProfile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_multi"))
                val humidityProfile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "humidity_multi"))

                val request =
                    DeviceTypeRequest(
                        objectId = "MULTI_SENSOR_001",
                        description = "Multi Sensor",
                        version = "1.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "Warning", DeviceEvent.DeviceLevel.WARNING, null),
                                DeviceEventRequest(null, "Danger", DeviceEvent.DeviceLevel.DANGER, null),
                            ),
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(tempProfile.id!!, 0.0, 50.0),
                                DeviceProfileTypeRequest(humidityProfile.id!!, 0.0, 100.0),
                            ),
                    )
                val createdId = deviceTypeService.create(request)
                val saved = deviceTypeRepository.findByIdWithAssociations(createdId)!!

                Then("각 DeviceProfile마다 EventSetting이 생성되고, 각 EventSetting마다 모든 DeviceEvent에 대한 EventCondition 생성") {
                    saved.deviceProfileTypes shouldHaveSize 2
                    saved.deviceEvents shouldHaveSize 3

                    saved.deviceProfileTypes.forEach { profileType ->
                        val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!)
                        eventSettings shouldHaveSize 1

                        val eventSetting = eventSettings.first()
                        eventSetting.conditions shouldHaveSize 3 // 3개 DeviceEvent
                    }
                }
            }
        }

        Given("1-2. Site → Feature → DeviceType 양방향 관계") {
            When("Site와 Feature를 DeviceType과 연결하면") {
                // 기존 FeatureServiceTest 로직 재사용
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "Building A"))
                val deviceType = deviceTypeRepository.saveAndFlush(com.pluxity.aiot.fixture.DeviceTypeFixture.create(objectId = "TYPE_INT_001"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            site = site,
                            deviceId = "DEVICE_INT_001",
                            objectId = "TYPE_INT_001",
                        ),
                    )

                Then("Feature.deviceType 양방향 관계가 설정된다") {
                    feature.deviceType?.id shouldBe deviceType.id
                    val savedDeviceType = deviceTypeRepository.findByIdWithAssociations(deviceType.id!!)!!
                    savedDeviceType.features.any { it.id == feature.id } shouldBe true

                    feature.site?.id shouldBe site.id
                    val savedSite = siteRepository.findById(site.id!!).get()
                    savedSite.features.any { it.id == feature.id } shouldBe true
                }
            }

            When("여러 Feature를 같은 DeviceType에 연결하면") {
                val site1 = siteRepository.saveAndFlush(SiteFixture.create(name = "Building B"))
                val site2 = siteRepository.saveAndFlush(SiteFixture.create(name = "Building C"))
                val deviceType = deviceTypeRepository.saveAndFlush(com.pluxity.aiot.fixture.DeviceTypeFixture.create(objectId = "TYPE_MULTI_FEATURE"))

                featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            site = site1,
                            deviceId = "DEVICE_MULTI_001",
                            objectId = "TYPE_MULTI_FEATURE",
                        ),
                    )
                featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            site = site2,
                            deviceId = "DEVICE_MULTI_002",
                            objectId = "TYPE_MULTI_FEATURE",
                        ),
                    )

                Then("DeviceType.features 컬렉션에 2개 포함") {
                    val savedDeviceType = deviceTypeRepository.findByIdWithAssociations(deviceType.id!!)!!
                    savedDeviceType.features.filter { it.objectId == "TYPE_MULTI_FEATURE" }.size shouldBe 2
                }
            }
        }

        Given("1-3. 복합 업데이트 - minValue/maxValue 전파") {
            When("DeviceType의 minValue/maxValue를 변경하면") {
                // 기존 DeviceTypeServiceTest 로직 재사용
                val profile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_update_int"))
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_INT",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = listOf(DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null)),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id!!, 0.0, 100.0)),
                    )
                val createdId = deviceTypeService.create(createRequest)

                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_INT",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = null,
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id, 10.0, 50.0)),
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("EventCondition의 minValue/maxValue가 업데이트된다") {
                    val updated = deviceTypeRepository.findByIdWithAssociations(createdId)!!
                    val profileType = updated.deviceProfileTypes.first()
                    val eventSetting = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!).first()
                    val condition = eventSetting.conditions.first()

                    condition.minValue shouldBe 10.0
                    condition.maxValue shouldBe 50.0
                }
            }

            When("DeviceType에 새 DeviceEvent를 추가하면") {
                val profile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_add_event"))
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_ADD_EVENT",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = listOf(DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null)),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id!!, 0.0, 100.0)),
                    )
                val createdId = deviceTypeService.create(createRequest)
                val saved = deviceTypeRepository.findByIdWithAssociations(createdId)!!
                val normalEventId = saved.deviceEvents.first { it.name == "Normal" }.id

                // 새 DeviceEvent 추가
                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_ADD_EVENT",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(normalEventId, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "Warning", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes = null,
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("DeviceEvent가 추가된다") {
                    val updated = deviceTypeRepository.findByIdWithAssociations(createdId)!!
                    updated.deviceEvents shouldHaveSize 2
                    updated.deviceEvents.any { it.name == "Warning" } shouldBe true
                }
            }

            When("DeviceType에 새 DeviceProfile을 추가하면") {
                val tempProfile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_add_profile"))
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_ADD_PROFILE",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = listOf(DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null)),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(tempProfile.id!!, 0.0, 100.0)),
                    )
                val createdId = deviceTypeService.create(createRequest)

                // 새 DeviceProfile 추가
                val humidityProfile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "humidity_add_profile"))
                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_ADD_PROFILE",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = null,
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(tempProfile.id, 0.0, 100.0),
                                DeviceProfileTypeRequest(humidityProfile.id!!, 0.0, 100.0),
                            ),
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("새 DeviceProfileType과 EventSetting이 자동 생성된다") {
                    val updated = deviceTypeRepository.findByIdWithAssociations(createdId)!!
                    updated.deviceProfileTypes shouldHaveSize 2

                    // 각 DeviceProfileType마다 EventSetting 존재
                    updated.deviceProfileTypes.forEach { profileType ->
                        val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!)
                        eventSettings shouldHaveSize 1
                        eventSettings.first().conditions shouldHaveSize 1 // 1개 DeviceEvent
                    }
                }
            }
        }

        Given("1-4. DeviceType cascade 삭제") {
            When("DeviceType을 삭제하면") {
                // 기존 DeviceTypeServiceTest 로직 재사용
                val profile = deviceProfileRepository.saveAndFlush(DeviceProfileFixture.create(fieldKey = "temp_delete_int"))
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_DELETE_INT",
                        description = "Sensor",
                        version = "1.0",
                        deviceEvents = listOf(DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null)),
                        deviceProfileTypes = listOf(DeviceProfileTypeRequest(profile.id!!, 0.0, 100.0)),
                    )
                val createdId = deviceTypeService.create(createRequest)
                val saved = deviceTypeRepository.findByIdWithAssociations(createdId)!!
                val eventIds = saved.deviceEvents.map { it.id!! }

                deviceTypeService.delete(createdId)

                Then("DeviceType, DeviceEvent이 cascade 삭제되고 DeviceProfile은 유지된다") {
                    shouldThrow<CustomException> { deviceTypeService.findById(createdId) }
                        .errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_TYPE

                    eventIds.forEach { eventId ->
                        deviceEventRepository.findById(eventId).isPresent shouldBe false
                    }

                    deviceProfileRepository.findById(profile.id).isPresent shouldBe true
                }
            }

            When("DeviceType 삭제하면 Feature도 cascade 삭제된다") {
                val deviceType = deviceTypeRepository.saveAndFlush(com.pluxity.aiot.fixture.DeviceTypeFixture.create(objectId = "TYPE_DELETE_FEATURE"))
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "Building B"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            site = site,
                            deviceId = "DEVICE_DELETE",
                            objectId = "TYPE_DELETE_FEATURE",
                        ),
                    )
                val featureId = feature.id!!

                deviceTypeService.delete(deviceType.id!!)

                Then("DeviceType cascade=ALL이므로 Feature도 삭제된다") {
                    featureRepository.findById(featureId).isPresent shouldBe false
                }
            }

            When("Site를 삭제하면") {
                val site = siteRepository.saveAndFlush(SiteFixture.create(name = "Building C"))
                val deviceType = deviceTypeRepository.saveAndFlush(com.pluxity.aiot.fixture.DeviceTypeFixture.create(objectId = "TYPE_SITE_DELETE"))
                val feature =
                    featureRepository.saveAndFlush(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            site = site,
                            deviceId = "DEVICE_SITE_DELETE",
                            objectId = "TYPE_SITE_DELETE",
                        ),
                    )
                val featureId = feature.id!!
                val siteId = site.id!!

                // Site는 cascade 없으므로 수동으로 Feature의 site를 null로 설정
                feature.site = null
                featureRepository.saveAndFlush(feature)

                siteRepository.deleteById(siteId)

                Then("Site는 삭제되고 Feature는 유지되며 site = null") {
                    siteRepository.findById(siteId).isPresent shouldBe false
                    featureRepository.findById(featureId).isPresent shouldBe true
                    featureRepository.findById(featureId).get().site.shouldBeNull()
                }
            }
        }
    })
