package com.pluxity.aiot.site

import com.pluxity.aiot.config.TestSecurityConfig
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.dto.SiteRequest
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
class SiteServiceTest(
    private val siteService: SiteService,
    private val siteRepository: SiteRepository,
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

        Given("Site 생성 기능") {
            When("유효한 Polygon으로 현장을 생성하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Test Site", validPolygon, "Test Description")

                val createdId = siteService.save(request)

                Then("성공적으로 생성되고 ID가 반환된다") {
                    createdId shouldNotBe null
                    val saved = siteRepository.findById(createdId).get()
                    saved.name shouldBe "Test Site"
                    saved.description shouldBe "Test Description"
                    saved.location shouldNotBe null
                }
            }

            When("잘못된 WKT(Polygon이 아님)로 현장을 생성하면") {
                val invalidPolygon = "POINT(127.0 37.0)"
                val request = SiteRequest("Test Site", invalidPolygon, "Test Description")

                val exception =
                    shouldThrow<CustomException> {
                        siteService.save(request)
                    }

                Then("INVALID_LOCATION 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.INVALID_LOCATION
                }
            }
        }

        Given("Site 조회 기능") {
            When("전체 현장을 조회하면") {
                // 사전 조건: 데이터 준비
                featureRepository.deleteAll()
                siteRepository.deleteAll()
                val validPolygon1 = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val validPolygon2 = "POLYGON((128.0 38.0, 128.1 38.0, 128.1 38.1, 128.0 38.1, 128.0 38.0))"
                siteService.save(SiteRequest("Site 1", validPolygon1, "Desc 1"))
                Thread.sleep(10)
                siteService.save(SiteRequest("Site 2", validPolygon2, "Desc 2"))

                val result = siteService.findAll()

                Then("생성일 내림차순으로 모든 현장이 반환된다") {
                    result shouldHaveSize 2
                    result[0].name shouldBe "Site 2"
                    result[1].name shouldBe "Site 1"
                }
            }

            When("존재하는 ID로 현장을 조회하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Test Site", validPolygon, "Test Description")
                val createdId = siteService.save(request)

                val result = siteService.findByIdResponse(createdId)

                Then("해당 현장이 반환된다") {
                    result.name shouldBe "Test Site"
                    result.description shouldBe "Test Description"
                }
            }

            When("존재하지 않는 ID로 현장을 조회하면") {
                val exception =
                    shouldThrow<CustomException> {
                        siteService.findByIdResponse(999999L)
                    }

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_SITE
                }
            }
        }

        Given("Site 업데이트 기능") {
            When("유효한 데이터로 현장을 업데이트하면") {
                val validPolygon1 = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request1 = SiteRequest("Original Site", validPolygon1, "Original Description")
                val createdId = siteService.save(request1)
                val validPolygon2 = "POLYGON((128.0 38.0, 128.1 38.0, 128.1 38.1, 128.0 38.1, 128.0 38.0))"
                val updateRequest = SiteRequest("Updated Site", validPolygon2, "Updated Description")

                siteService.putUpdate(createdId, updateRequest)

                Then("성공적으로 업데이트된다") {
                    val updated = siteService.findByIdResponse(createdId)
                    updated.name shouldBe "Updated Site"
                    updated.description shouldBe "Updated Description"
                }
            }

            When("빈 name으로 현장을 업데이트하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Original Site", validPolygon, "Original Description")
                val createdId = siteService.save(request)
                val updateRequest = SiteRequest("", validPolygon, "Updated Description")

                Then("IllegalArgumentException이 발생한다") {
                    shouldThrow<IllegalArgumentException> {
                        siteService.putUpdate(createdId, updateRequest)
                    }
                }
            }
        }

        Given("Site 삭제 기능") {
            When("존재하는 현장을 삭제하면") {
                val validPolygon = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))"
                val request = SiteRequest("Test Site", validPolygon, "Test Description")
                val createdId = siteService.save(request)

                siteService.delete(createdId)

                Then("성공적으로 삭제된다") {
                    siteRepository.findById(createdId).isPresent shouldBe false
                }
            }

            When("존재하지 않는 현장을 삭제하면") {
                val exception =
                    shouldThrow<CustomException> {
                        siteService.delete(999999L)
                    }

                Then("NOT_FOUND_SITE 예외가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_SITE
                }
            }
        }
    })
