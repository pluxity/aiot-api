package com.pluxity.aiot.facility

import com.pluxity.aiot.config.TestSecurityConfig
import com.pluxity.aiot.facility.dto.FacilityRequest
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FacilityServiceTest(
    private val facilityService: FacilityService,
    private val facilityRepository: FacilityRepository,
    @Autowired private val featureRepository: FeatureRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        beforeEach {
            TestSecurityConfig.setAdminAuthentication()
        }

        afterEach {
            TestSecurityConfig.clearAuthentication()
        }

        Given("Facility 생성 기능") {
            When("유효한 Polygon으로 시설을 생성하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = FacilityRequest("Test Facility", validPolygon, "Test Description")

                val createdId = facilityService.save(request)

                Then("성공적으로 생성되고 ID가 반환된다") {
                    createdId shouldNotBe null
                    val saved = facilityRepository.findById(createdId).get()
                    saved.name shouldBe "Test Facility"
                    saved.description shouldBe "Test Description"
                    saved.location shouldNotBe null
                }
            }

            When("잘못된 WKT(Polygon이 아님)로 시설을 생성하면") {
                val invalidPolygon = "POINT(127.0 37.0)"
                val request = FacilityRequest("Test Facility", invalidPolygon, "Test Description")

                val exception =
                    shouldThrow<CustomException> {
                        facilityService.save(request)
                    }

                Then("INVALID_LOCATION 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.INVALID_LOCATION
                }
            }
        }

        Given("Facility 조회 기능") {
            When("전체 시설을 조회하면") {
                // 사전 조건: 데이터 준비
                featureRepository.deleteAll()
                facilityRepository.deleteAll()
                val validPolygon1 = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val validPolygon2 = "POLYGON((128.0 38.0, 128.1 38.0, 128.1 38.1, 128.0 38.1, 128.0 38.0))"
                facilityService.save(FacilityRequest("Facility 1", validPolygon1, "Desc 1"))
                Thread.sleep(10)
                facilityService.save(FacilityRequest("Facility 2", validPolygon2, "Desc 2"))

                val result = facilityService.findAll()

                Then("생성일 내림차순으로 모든 시설이 반환된다") {
                    result shouldHaveSize 2
                    result[0].name shouldBe "Facility 2"
                    result[1].name shouldBe "Facility 1"
                }
            }

            When("존재하는 ID로 시설을 조회하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = FacilityRequest("Test Facility", validPolygon, "Test Description")
                val createdId = facilityService.save(request)

                val result = facilityService.findByIdResponse(createdId)

                Then("해당 시설이 반환된다") {
                    result.name shouldBe "Test Facility"
                    result.description shouldBe "Test Description"
                }
            }

            When("존재하지 않는 ID로 시설을 조회하면") {
                val exception =
                    shouldThrow<CustomException> {
                        facilityService.findByIdResponse(999999L)
                    }

                Then("NOT_FOUND_FACILITY 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_FACILITY
                }
            }
        }

        Given("Facility 업데이트 기능") {
            When("유효한 데이터로 시설을 업데이트하면") {
                val validPolygon1 = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request1 = FacilityRequest("Original Facility", validPolygon1, "Original Description")
                val createdId = facilityService.save(request1)
                val validPolygon2 = "POLYGON((128.0 38.0, 128.1 38.0, 128.1 38.1, 128.0 38.1, 128.0 38.0))"
                val updateRequest = FacilityRequest("Updated Facility", validPolygon2, "Updated Description")

                facilityService.putUpdate(createdId, updateRequest)

                Then("성공적으로 업데이트된다") {
                    val updated = facilityService.findByIdResponse(createdId)
                    updated.name shouldBe "Updated Facility"
                    updated.description shouldBe "Updated Description"
                }
            }

            When("빈 name으로 시설을 업데이트하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = FacilityRequest("Original Facility", validPolygon, "Original Description")
                val createdId = facilityService.save(request)
                val updateRequest = FacilityRequest("", validPolygon, "Updated Description")

                Then("IllegalArgumentException이 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        facilityService.putUpdate(createdId, updateRequest)
                    }
                }
            }
        }

        Given("Facility 삭제 기능") {
            When("존재하는 시설을 삭제하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = FacilityRequest("Test Facility", validPolygon, "Test Description")
                val createdId = facilityService.save(request)

                facilityService.deleteFacility(createdId)

                Then("성공적으로 삭제된다") {
                    facilityRepository.findById(createdId).isPresent shouldBe false
                }
            }

            When("존재하지 않는 시설을 삭제하면") {
                val exception =
                    shouldThrow<CustomException> {
                        facilityService.deleteFacility(999999L)
                    }

                Then("NOT_FOUND_FACILITY 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_FACILITY
                }
            }
        }
    })