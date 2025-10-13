package com.pluxity.aiot.feature

import com.pluxity.aiot.facility.FacilityRepository
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
import com.pluxity.aiot.fixture.DeviceTypeFixture
import com.pluxity.aiot.fixture.FacilityFixture
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeatureServiceTest(
    private val featureService: FeatureService,
    private val featureRepository: FeatureRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val facilityRepository: FacilityRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("Feature 조회 기능") {
            When("전체 Feature를 조회하면") {
                // 사전 조건: 데이터 준비
                val deviceType1 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_001"))
                val deviceType2 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_002"))

                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(deviceType = deviceType1, deviceId = "DEVICE_001", objectId = "TYPE_001", name = "Device 1"),
                        FeatureFixture.create(deviceType = deviceType2, deviceId = "DEVICE_002", objectId = "TYPE_002", name = "Device 2"),
                        FeatureFixture.create(deviceType = deviceType1, deviceId = "DEVICE_003", objectId = "TYPE_001", name = "Device 3"),
                    ),
                )

                val result = featureService.findAll()

                Then("모든 Feature가 반환된다") {
                    result shouldHaveSize 3
                }
            }

            When("facilityId로 필터링하면") {
                // 사전 조건: Facility와 Feature 생성
                val facility1 = facilityRepository.save(FacilityFixture.create(name = "Facility 1"))
                val facility2 = facilityRepository.save(FacilityFixture.create(name = "Facility 2"))
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_FAC_001"))

                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            facility = facility1,
                            deviceId = "DEVICE_FAC_001",
                            objectId = "OBJ_FAC_001",
                            name = "Device Fac 1",
                        ),
                        FeatureFixture.create(
                            deviceType = deviceType,
                            facility = facility1,
                            deviceId = "DEVICE_FAC_002",
                            objectId = "OBJ_FAC_002",
                            name = "Device Fac 2",
                        ),
                        FeatureFixture.create(
                            deviceType = deviceType,
                            facility = facility2,
                            deviceId = "DEVICE_FAC_003",
                            objectId = "OBJ_FAC_003",
                            name = "Device Fac 3",
                        ),
                    ),
                )

                val searchCondition = FeatureSearchCondition(facilityId = facility1.id!!)
                val result = featureService.findAll(searchCondition)

                Then("해당 Facility의 Feature만 반환된다") {
                    result shouldHaveSize 2
                    result.all { it.facilityResponse?.id == facility1.id } shouldBe true
                }
            }

            When("deviceTypeId로 필터링하면") {
                // 사전 조건
                val deviceType1 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_FILTER_001"))
                val deviceType2 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_FILTER_002"))

                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(deviceType = deviceType1, deviceId = "DEVICE_TYPE_001", objectId = "TYPE_FILTER_001"),
                        FeatureFixture.create(deviceType = deviceType1, deviceId = "DEVICE_TYPE_002", objectId = "TYPE_FILTER_001"),
                        FeatureFixture.create(deviceType = deviceType2, deviceId = "DEVICE_TYPE_003", objectId = "TYPE_FILTER_002"),
                    ),
                )

                val searchCondition = FeatureSearchCondition(deviceTypeId = deviceType1.id!!)
                val result = featureService.findAll(searchCondition)

                Then("해당 DeviceType의 Feature만 반환된다") {
                    result shouldHaveSize 2
                    result.all { it.deviceType.id == deviceType1.id } shouldBe true
                }
            }

            When("isActive로 필터링하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_ACTIVE_FILTER"))
                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_ACTIVE_001",
                            objectId = "OBJ_ACTIVE_001",
                            name = "Active 1",
                            isActive = true,
                        ),
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_ACTIVE_002",
                            objectId = "OBJ_ACTIVE_002",
                            name = "Active 2",
                            isActive = true,
                        ),
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_INACTIVE_001",
                            objectId = "OBJ_INACTIVE_001",
                            name = "Inactive 1",
                            isActive = false,
                        ),
                    ),
                )

                val searchCondition = FeatureSearchCondition(isActive = true)
                val result = featureService.findAll(searchCondition)

                Then("활성화된 Feature만 반환된다") {
                    result.filter { it.objectId.startsWith("OBJ_ACTIVE") || it.objectId.startsWith("OBJ_INACTIVE") } shouldHaveSize 2
                    result.filter { it.objectId.startsWith("OBJ_ACTIVE") }.all { it.isActive } shouldBe true
                }
            }

            When("deviceId로 필터링하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_SEARCH"))
                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_SEARCH_001",
                            objectId = "OBJ_SEARCH_001",
                            name = "Search 1",
                        ),
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_SEARCH_002",
                            objectId = "OBJ_SEARCH_002",
                            name = "Search 2",
                        ),
                    ),
                )

                val searchCondition = FeatureSearchCondition(deviceId = "DEVICE_SEARCH_001")
                val result = featureService.findAll(searchCondition)

                Then("해당 deviceId의 Feature만 반환된다") {
                    result shouldHaveSize 1
                    result.first().deviceId shouldBe "DEVICE_SEARCH_001"
                }
            }
        }

        Given("Feature 업데이트 기능") {
            When("DeviceType을 변경하면") {
                // 사전 조건
                val deviceType1 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_UPDATE_001"))
                val deviceType2 = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_UPDATE_002"))
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(deviceType = deviceType1, deviceId = "DEVICE_UPDATE_001", objectId = "TYPE_UPDATE_001"),
                    )

                val updateRequest = FeatureUpdateRequest(deviceTypeId = deviceType2.id!!, isActive = true)
                featureService.updateFeature(feature.id!!, updateRequest)

                Then("DeviceType이 변경된다") {
                    val updated = featureRepository.findById(feature.id!!).get()
                    updated.deviceType?.id shouldBe deviceType2.id
                }
            }

            When("isActive를 변경하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_ACTIVE_001"))
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_ACTIVE_UPDATE",
                            objectId = "TYPE_ACTIVE_001",
                            isActive = true,
                        ),
                    )

                val updateRequest = FeatureUpdateRequest(deviceTypeId = deviceType.id!!, isActive = false)
                featureService.updateFeature(feature.id!!, updateRequest)

                Then("isActive가 변경된다") {
                    val updated = featureRepository.findById(feature.id!!).get()
                    updated.isActive shouldBe false
                }
            }

            When("존재하지 않는 Feature를 업데이트하면") {
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_NOT_FOUND"))
                val updateRequest = FeatureUpdateRequest(deviceTypeId = deviceType.id!!, isActive = true)

                val exception =
                    shouldThrow<CustomException> {
                        featureService.updateFeature(999999L, updateRequest)
                    }

                Then("NOT_FOUND_FEATURE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_FEATURE
                }
            }

            When("존재하지 않는 DeviceType으로 업데이트하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_EXIST"))
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(deviceType = deviceType, deviceId = "DEVICE_INVALID_TYPE", objectId = "TYPE_EXIST"),
                    )

                val updateRequest = FeatureUpdateRequest(deviceTypeId = 999999L, isActive = true)

                val exception =
                    shouldThrow<CustomException> {
                        featureService.updateFeature(feature.id!!, updateRequest)
                    }

                Then("NOT_FOUND_DEVICE_TYPE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_TYPE
                }
            }
        }

        Given("Feature 이름 업데이트 기능") {
            When("Feature 이름을 업데이트하면") {
                // 사전 조건
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(deviceId = "DEVICE_NAME_UPDATE", objectId = "OBJ_NAME_UPDATE", name = "Original Name"),
                    )

                featureService.updateFeatureName(feature.id!!, "Updated Name")

                Then("이름이 변경된다") {
                    val updated = featureRepository.findById(feature.id!!).get()
                    updated.name shouldBe "Updated Name"
                }
            }

            When("존재하지 않는 Feature의 이름을 업데이트하면") {
                val exception =
                    shouldThrow<CustomException> {
                        featureService.updateFeatureName(999999L, "New Name")
                    }

                Then("NOT_FOUND_FEATURE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_FEATURE
                }
            }
        }

        Given("Feature deviceId 조회 기능") {
            When("존재하는 deviceId로 조회하면") {
                // 사전 조건
                val deviceType = deviceTypeRepository.save(DeviceTypeFixture.create(objectId = "TYPE_DEVICE_ID"))
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(
                            deviceType = deviceType,
                            deviceId = "DEVICE_ID_001",
                            objectId = "TYPE_DEVICE_ID",
                            name = "Device by ID",
                        ),
                    )

                val result = featureService.findByDeviceIdResponse(feature.deviceId)

                Then("해당 Feature가 반환된다") {
                    result.deviceId shouldBe "DEVICE_ID_001"
                    result.name shouldBe "Device by ID"
                }
            }

            When("존재하지 않는 deviceId로 조회하면") {
                val exception =
                    shouldThrow<CustomException> {
                        featureService.findByDeviceIdResponse("NON_EXISTENT_DEVICE_ID")
                    }

                Then("NOT_FOUND_DEVICE_BY_FEATURE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_BY_FEATURE
                }
            }
        }
    })
