package com.pluxity.aiot.feature

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import com.linecorp.kotlinjdsl.querymodel.jpql.JpqlQueryable
import com.linecorp.kotlinjdsl.querymodel.jpql.select.SelectQuery
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
import com.pluxity.aiot.feature.entity.dummyFeature
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class FeatureServiceKoTest :
    BehaviorSpec({
        val featureRepository: FeatureRepository = mockk()
        val featureService = FeatureService(featureRepository)

        Given("Feature 조회 기능") {
            When("전체 Feature를 조회하면") {
                val features =
                    listOf(
                        dummyFeature(
                            id = 1L,
                            deviceId = "DEVICE_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 1",
                        ),
                        dummyFeature(
                            id = 2L,
                            deviceId = "DEVICE_002",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 2",
                        ),
                        dummyFeature(
                            id = 3L,
                            deviceId = "DEVICE_003",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Device 3",
                        ),
                    )

                every {
                    featureRepository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                    )
                } returns features

                val result = featureService.findAll()

                Then("모든 Feature가 반환된다") {
                    result shouldHaveSize 3
                    result.map { it.deviceId } shouldBe listOf("DEVICE_001", "DEVICE_002", "DEVICE_003")
                    verify {
                        featureRepository.findAll(
                            init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                        )
                    }
                }
            }

            When("검색 조건을 전달해도 조회 결과가 매핑된다") {
                val features =
                    listOf(
                        dummyFeature(
                            id = 10L,
                            deviceId = "DEVICE_SEARCH_001",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = "Search 1",
                        ),
                    )

                every {
                    featureRepository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                    )
                } returns features

                val searchCondition = FeatureSearchCondition(deviceId = "DEVICE_SEARCH_001")
                val result = featureService.findAll(searchCondition)

                Then("조건에 맞는 Feature가 반환된다") {
                    result shouldHaveSize 1
                    result.first().deviceId shouldBe "DEVICE_SEARCH_001"
                    verify {
                        featureRepository.findAll(
                            init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                        )
                    }
                }
            }

            When("name이 null인 Feature가 포함되면") {
                val features =
                    listOf(
                        dummyFeature(
                            id = 20L,
                            deviceId = "DEVICE_NULL_NAME",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            name = null,
                        ),
                    )

                every {
                    featureRepository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                    )
                } returns features

                Then("응답 매핑 과정에서 예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        featureService.findAll()
                    }
                }
            }

            When("좌표가 null인 Feature가 포함되면") {
                val features =
                    listOf(
                        dummyFeature(
                            id = 21L,
                            deviceId = "DEVICE_NULL_COORD",
                            objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                            longitude = null,
                            latitude = null,
                        ),
                    )

                every {
                    featureRepository.findAll(
                        init = any<Jpql.() -> JpqlQueryable<SelectQuery<Feature>>>(),
                    )
                } returns features

                Then("응답 매핑 과정에서 예외가 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        featureService.findAll()
                    }
                }
            }
        }

        Given("Feature 업데이트 기능") {

            When("isActive를 변경하면") {
                val feature =
                    dummyFeature(
                        id = 1L,
                        deviceId = "DEVICE_ACTIVE_UPDATE",
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        isActive = true,
                    )

                every { featureRepository.findByIdOrNull(feature.requiredId) } returns feature

                val updateRequest = FeatureUpdateRequest(active = false, height = 5.0)
                featureService.updateFeature(feature.requiredId, updateRequest)

                Then("isActive와 height가 변경된다") {
                    feature.isActive shouldBe false
                    feature.height shouldBe 5.0
                    verify { featureRepository.findByIdOrNull(feature.requiredId) }
                }
            }

            When("존재하지 않는 Feature를 업데이트하면") {
                val updateRequest = FeatureUpdateRequest(active = true, height = 5.0)

                every { featureRepository.findByIdOrNull(999999L) } returns null

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
                val feature =
                    dummyFeature(
                        id = 2L,
                        deviceId = "DEVICE_NAME_UPDATE",
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        name = "Original Name",
                    )

                every { featureRepository.findByIdOrNull(feature.requiredId) } returns feature

                featureService.updateFeatureName(feature.requiredId, "Updated Name")

                Then("이름이 변경된다") {
                    feature.name shouldBe "Updated Name"
                    verify { featureRepository.findByIdOrNull(feature.requiredId) }
                }
            }

            When("존재하지 않는 Feature의 이름을 업데이트하면") {
                every { featureRepository.findByIdOrNull(999999L) } returns null

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
                val feature =
                    dummyFeature(
                        id = 3L,
                        deviceId = "DEVICE_ID_001",
                        objectId = SensorType.TEMPERATURE_HUMIDITY.objectId,
                        name = "Device by ID",
                    )

                every { featureRepository.findByDeviceId("DEVICE_ID_001") } returns feature

                val result = featureService.findByDeviceIdResponse(feature.deviceId)

                Then("해당 Feature가 반환된다") {
                    result.deviceId shouldBe "DEVICE_ID_001"
                    result.name shouldBe "Device by ID"
                }
            }

            When("존재하지 않는 deviceId로 조회하면") {
                every { featureRepository.findByDeviceId("NON_EXISTENT_DEVICE_ID") } returns null

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
