package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.fixture.DeviceProfileFixture
import com.pluxity.aiot.fixture.DeviceTypeFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.event.DeviceEventRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.dto.DeviceEventRequest
import com.pluxity.aiot.system.device.type.dto.DeviceProfileTypeRequest
import com.pluxity.aiot.system.device.type.dto.DeviceTypeRequest
import com.pluxity.aiot.system.event.setting.EventSettingRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeviceTypeServiceTest(
    private val deviceTypeService: DeviceTypeService,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val deviceEventRepository: DeviceEventRepository,
    private val eventSettingRepository: EventSettingRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("DeviceType 생성 기능") {
            When("기본 정보만으로 DeviceType을 생성하면") {
                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_001",
                        description = "Test Sensor",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes = null,
                    )

                val createdId = deviceTypeService.create(request)

                Then("성공적으로 생성되고 ID가 반환된다") {
                    createdId shouldNotBe null
                    val saved = deviceTypeRepository.findById(createdId).get()
                    saved.objectId shouldBe "SENSOR_001"
                    saved.description shouldBe "Test Sensor"
                    saved.version shouldBe "1.0.0"
                    saved.deviceEvents shouldHaveSize 0
                    saved.deviceProfileTypes shouldHaveSize 0
                }
            }

            When("DeviceType과 DeviceEvent를 함께 생성하면") {
                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_002",
                        description = "Temperature Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "Warning", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes = null,
                    )

                val createdId = deviceTypeService.create(request)

                Then("DeviceType과 DeviceEvent가 모두 생성된다") {
                    val saved = deviceTypeRepository.findById(createdId).get()
                    saved.deviceEvents shouldHaveSize 2
                    saved.deviceEvents.map { it.name }.toSet() shouldBe setOf("Normal", "Warning")
                }
            }

            When("DeviceType과 DeviceProfile을 연결하면") {
                // 사전 조건: DeviceProfile 생성
                val tempProfile = DeviceProfileFixture.create(fieldKey = "temp_profile3")
                val savedProfile = deviceProfileRepository.saveAndFlush(tempProfile)

                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_003",
                        description = "Sensor with Profile",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 0.0, 100.0),
                            ),
                    )

                val createdId = deviceTypeService.create(request)

                Then("DeviceProfileType이 생성된다") {
                    val saved = deviceTypeRepository.findById(createdId).get()
                    saved.deviceProfileTypes shouldHaveSize 1
                    val profileType = saved.deviceProfileTypes.first()
                    profileType.deviceProfile?.id shouldBe savedProfile.id
                }
            }

            When("DeviceType, DeviceEvent, DeviceProfile을 모두 생성하면") {
                // 사전 조건: DeviceProfile 생성
                val tempProfile = DeviceProfileFixture.create(fieldKey = "temp_profile4")
                val savedProfile = deviceProfileRepository.saveAndFlush(tempProfile)

                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_004",
                        description = "Full Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "Warning", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 0.0, 100.0),
                            ),
                    )

                val createdId = deviceTypeService.create(request)

                Then("EventSetting과 EventCondition이 자동 생성된다") {
                    val saved = deviceTypeRepository.findById(createdId).get()
                    saved.deviceEvents shouldHaveSize 2
                    saved.deviceProfileTypes shouldHaveSize 1

                    // EventSetting 확인
                    val profileType = saved.deviceProfileTypes.first()
                    val eventSettings = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!)
                    eventSettings shouldHaveSize 1

                    // EventCondition 확인 (각 DeviceEvent마다 생성됨)
                    val eventSetting = eventSettings.first()
                    eventSetting.conditions shouldHaveSize 2
                }
            }

            When("minValue가 maxValue보다 크면") {
                val tempProfile = DeviceProfileFixture.create(fieldKey = "temp_profile5")
                val savedProfile = deviceProfileRepository.saveAndFlush(tempProfile)

                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_005",
                        description = "Invalid Sensor",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 100.0, 0.0),
                            ),
                    )

                val exception =
                    shouldThrow<CustomException> {
                        deviceTypeService.create(request)
                    }

                Then("INVALID_PROFILE_MIN_MAX_VALUE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.INVALID_PROFILE_MIN_MAX_VALUE
                }
            }
        }

        Given("DeviceType 조회 기능") {
            When("전체 DeviceType을 조회하면") {
                // 사전 조건: 데이터 준비
                deviceTypeRepository.deleteAll()
                deviceTypeRepository.saveAll(
                    listOf(
                        DeviceTypeFixture.create(objectId = "QUERY_SENSOR_1", description = "Query Sensor 1"),
                        DeviceTypeFixture.create(objectId = "QUERY_SENSOR_2", description = "Query Sensor 2"),
                    ),
                )

                val result = deviceTypeService.findAll()

                Then("모든 DeviceType이 반환된다") {
                    result shouldHaveSize 2
                }
            }

            When("ID로 DeviceType을 조회하면") {
                val saved = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "QUERY_SENSOR_3", description = "Query Sensor 3"))

                val result = deviceTypeService.getById(saved.id!!)

                Then("해당 DeviceType이 반환된다") {
                    result.objectId shouldBe "QUERY_SENSOR_3"
                    result.description shouldBe "Query Sensor 3"
                }
            }

            When("존재하지 않는 ID로 조회하면") {
                val exception =
                    shouldThrow<CustomException> {
                        deviceTypeService.getById(999999L)
                    }

                Then("NOT_FOUND_DEVICE_TYPE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_TYPE
                }
            }

            When("DeviceType의 연결된 프로필을 조회하면") {
                // 사전 조건: DeviceProfile과 DeviceType 생성
                val tempProfile = DeviceProfileFixture.create(fieldKey = "temp_profile_query")
                val savedProfile = deviceProfileRepository.saveAndFlush(tempProfile)

                val request =
                    DeviceTypeRequest(
                        objectId = "SENSOR_PROFILE",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 0.0, 100.0),
                            ),
                    )
                val createdId = deviceTypeService.create(request)

                val result = deviceTypeService.findProfilesByDeviceTypeId(createdId)

                Then("연결된 프로필이 반환된다") {
                    result shouldHaveSize 1
                    result.first().fieldKey shouldBe "temp_profile_query"
                }
            }
        }

        Given("DeviceType 업데이트 기능") {
            When("기본 정보만 업데이트하면") {
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_1",
                        description = "Original Sensor",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes = null,
                    )
                val createdId = deviceTypeService.create(createRequest)

                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_1",
                        description = "Updated Sensor",
                        version = "2.0.0",
                        deviceEvents = null,
                        deviceProfileTypes = null,
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("기본 정보가 업데이트된다") {
                    val updated = deviceTypeService.getById(createdId)
                    updated.description shouldBe "Updated Sensor"
                    updated.version shouldBe "2.0.0"
                }
            }

            When("DeviceEvent를 추가하면") {
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_2",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "NormalUpdate2", DeviceEvent.DeviceLevel.NORMAL, null),
                            ),
                        deviceProfileTypes = null,
                    )
                val createdId = deviceTypeService.create(createRequest)

                // 생성된 이벤트 ID 조회
                val normalEventId = deviceEventRepository.findAll().first { it.name == "NormalUpdate2" && it.deviceType?.id == createdId }.id

                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_2",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(normalEventId, "NormalUpdate2", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "WarningUpdate2", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes = null,
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("새 DeviceEvent가 추가된다") {
                    // Repository에서 직접 확인
                    val events = deviceEventRepository.findAll().filter { it.deviceType?.id == createdId }
                    events shouldHaveSize 2
                    events.map { it.name }.toSet() shouldBe setOf("NormalUpdate2", "WarningUpdate2")
                }
            }

            When("DeviceEvent를 제거하면") {
                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_3",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "NormalUpdate3", DeviceEvent.DeviceLevel.NORMAL, null),
                                DeviceEventRequest(null, "WarningUpdate3", DeviceEvent.DeviceLevel.WARNING, null),
                            ),
                        deviceProfileTypes = null,
                    )
                val createdId = deviceTypeService.create(createRequest)
                val normalEventId =
                    deviceEventRepository.findAll().first { it.name == "NormalUpdate3" && it.deviceType?.id == createdId }.id

                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_3",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(normalEventId, "NormalUpdate3", DeviceEvent.DeviceLevel.NORMAL, null),
                            ),
                        deviceProfileTypes = null,
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("DeviceEvent가 제거되고 관련 EventCondition도 삭제된다") {
                    // Repository에서 직접 확인
                    val events = deviceEventRepository.findAll().filter { it.deviceType?.id == createdId }
                    events shouldHaveSize 1
                    events.first().name shouldBe "NormalUpdate3"
                }
            }

            When("minValue와 maxValue를 업데이트하면") {
                val profile = DeviceProfileFixture.create(fieldKey = "temp_update_minmax")
                val savedProfile = deviceProfileRepository.saveAndFlush(profile)

                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_4",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                            ),
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 0.0, 100.0),
                            ),
                    )
                val createdId = deviceTypeService.create(createRequest)

                val updateRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_UPDATE_4",
                        description = "Sensor",
                        version = "1.0.0",
                        deviceEvents = null,
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 10.0, 50.0),
                            ),
                    )
                deviceTypeService.update(createdId, updateRequest)

                Then("minValue와 maxValue가 업데이트된다") {
                    val updated = deviceTypeRepository.findById(createdId).get()
                    val profileType = updated.deviceProfileTypes.first()
                    val eventSetting = eventSettingRepository.findAllByDeviceProfileTypeId(profileType.id!!).first()
                    val condition = eventSetting.conditions.first()
                    condition.minValue shouldBe 10.0
                    condition.maxValue shouldBe 50.0
                }
            }
        }

        Given("DeviceType 삭제 기능") {
            When("DeviceType을 삭제하면") {
                val profile = DeviceProfileFixture.create(fieldKey = "temp_delete")
                val savedProfile = deviceProfileRepository.saveAndFlush(profile)

                val createRequest =
                    DeviceTypeRequest(
                        objectId = "SENSOR_DELETE",
                        description = "Sensor to Delete",
                        version = "1.0.0",
                        deviceEvents =
                            listOf(
                                DeviceEventRequest(null, "Normal", DeviceEvent.DeviceLevel.NORMAL, null),
                            ),
                        deviceProfileTypes =
                            listOf(
                                DeviceProfileTypeRequest(savedProfile.id!!, 0.0, 100.0),
                            ),
                    )
                val createdId = deviceTypeService.create(createRequest)

                deviceTypeService.delete(createdId)

                Then("DeviceType과 모든 연관 데이터가 cascade 삭제된다") {
                    deviceTypeRepository.findById(createdId).isPresent shouldBe false
                    // DeviceEvent도 cascade 삭제 확인
                    val events = deviceEventRepository.findAll().filter { it.deviceType?.id == createdId }
                    events shouldHaveSize 0
                }
            }

            When("존재하지 않는 DeviceType을 삭제하면") {
                val exception =
                    shouldThrow<CustomException> {
                        deviceTypeService.delete(999999L)
                    }

                Then("NOT_FOUND_DEVICE_TYPE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_TYPE
                }
            }
        }
    })