package com.pluxity.aiot.feature

import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.SiteRepository
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
    private val siteRepository: SiteRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("Feature 조회 기능") {
            featureRepository.deleteAll()
            When("전체 Feature를 조회하면") {
                // 사전 조건: 데이터 준비
                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceId = "DEVICE_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 1",
                        ),
                        FeatureFixture.create(
                            deviceId = "DEVICE_002",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 2",
                        ),
                        FeatureFixture.create(
                            deviceId = "DEVICE_003",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 3",
                        ),
                    ),
                )

                val result = featureService.findAll()

                Then("모든 Feature가 반환된다") {
                    result shouldHaveSize 3
                }
            }

            When("siteId로 필터링하면") {
                // 사전 조건: Site와 Feature 생성
                val site1 = siteRepository.save(SiteFixture.create(name = "Site 1"))
                val site2 = siteRepository.save(SiteFixture.create(name = "Site 2"))

                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            site = site1,
                            deviceId = "DEVICE_FAC_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device Fac 1",
                        ),
                        FeatureFixture.create(
                            site = site1,
                            deviceId = "DEVICE_FAC_002",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device Fac 2",
                        ),
                        FeatureFixture.create(
                            site = site2,
                            deviceId = "DEVICE_FAC_003",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device Fac 3",
                        ),
                    ),
                )

                val searchCondition = FeatureSearchCondition(siteId = site1.id!!)
                val result = featureService.findAll(searchCondition)

                Then("해당 Site의 Feature만 반환된다") {
                    result shouldHaveSize 2
                    result.all { it.siteResponse?.id == site1.id } shouldBe true
                }
            }

            When("objectId로 필터링하면") {
                // 사전 조건

                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(deviceId = "DEVICE_TYPE_001", objectId = SensorType.TEMPERATURE_HUMIDITY.objectId),
                        FeatureFixture.create(deviceId = "DEVICE_TYPE_002", objectId = SensorType.TEMPERATURE_HUMIDITY.objectId),
                        FeatureFixture.create(deviceId = "DEVICE_TYPE_003", objectId = SensorType.FIRE.objectId),
                    ),
                )

                val searchCondition = FeatureSearchCondition(objectId = SensorType.TEMPERATURE_HUMIDITY.objectId)
                val result = featureService.findAll(searchCondition)

                Then("해당 DeviceType의 Feature만 반환된다") {
                    result shouldHaveSize 2
                    result.all { it.objectId == SensorType.TEMPERATURE_HUMIDITY.objectId } shouldBe true
                }
            }

            When("isActive로 필터링하면") {
                // 사전 조건
                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceId = "DEVICE_ACTIVE_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Active 1",
                            isActive = true,
                        ),
                        FeatureFixture.create(
                            deviceId = "DEVICE_ACTIVE_002",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Active 2",
                            isActive = true,
                        ),
                        FeatureFixture.create(
                            deviceId = "DEVICE_INACTIVE_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Inactive 1",
                            isActive = false,
                        ),
                    ),
                )

                val searchCondition = FeatureSearchCondition(isActive = true)
                val result = featureService.findAll(searchCondition)

                Then("활성화된 Feature만 반환된다") {
                    result.filter { it.deviceId.startsWith("DEVICE_ACTIVE") || it.deviceId.startsWith("DEVICE_INACTIVE") } shouldHaveSize 2
                    result.filter { it.deviceId.startsWith("DEVICE_INACTIVE") }.all { it.isActive } shouldBe true
                }
            }

            When("deviceId로 필터링하면") {
                // 사전 조건
                featureRepository.saveAll(
                    listOf(
                        FeatureFixture.create(
                            deviceId = "DEVICE_SEARCH_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Search 1",
                        ),
                        FeatureFixture.create(
                            deviceId = "DEVICE_SEARCH_002",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
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

            When("isActive를 변경하면") {
                // 사전 조건
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(
                            deviceId = "DEVICE_ACTIVE_UPDATE",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            isActive = true,
                        ),
                    )

                val updateRequest = FeatureUpdateRequest(active = false, height = 5.0)
                featureService.updateFeature(feature.id!!, updateRequest)

                Then("isActive가 변경된다") {
                    val updated = featureRepository.findById(feature.id!!).get()
                    updated.isActive shouldBe false
                }
            }

            When("존재하지 않는 Feature를 업데이트하면") {
                val updateRequest = FeatureUpdateRequest(active = true, height = 5.0)

                val exception =
                    shouldThrow<CustomException> {
                        featureService.updateFeature(999999L, updateRequest)
                    }

                Then("NOT_FOUND_FEATURE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_FEATURE
                }
            }
        }

        Given("Feature 이름 업데이트 기능") {
            When("Feature 이름을 업데이트하면") {
                // 사전 조건
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(
                            deviceId = "DEVICE_NAME_UPDATE",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Original Name",
                        ),
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
                val feature =
                    featureRepository.save(
                        FeatureFixture.create(
                            deviceId = "DEVICE_ID_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
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
