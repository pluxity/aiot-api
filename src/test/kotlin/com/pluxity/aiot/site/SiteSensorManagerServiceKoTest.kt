package com.pluxity.aiot.site

import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.user.entity.User
import com.pluxity.aiot.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private var sequence = 0

private fun user(
    name: String,
    phoneNumber: String? = "010-1111-2222",
) = User(
    username = "manager${sequence++}",
    password = "pw",
    name = name,
    code = null,
    phoneNumber = phoneNumber,
    department = "안전관리",
)

@SpringBootTest
@ActiveProfiles("test")
class SiteSensorManagerServiceKoTest(
    private val siteSensorManagerService: SiteSensorManagerService,
    private val siteSensorManagerRepository: SiteSensorManagerRepository,
    private val siteRepository: SiteRepository,
    private val siteService: SiteService,
    private val userRepository: UserRepository,
) : BehaviorSpec({
        extension(SpringExtension)

        // 다른 스펙이 남긴 site/feature를 건드리면 FK에 걸리므로 매핑만 정리한다
        afterEach { siteSensorManagerRepository.deleteAll() }

        Given("현장의 카테고리 담당자를 지정") {
            When("여러 명을 지정") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))
                val lee = userRepository.save(user("이담당"))

                siteSensorManagerService.replace(
                    site.requiredId,
                    SensorType.FIRE,
                    listOf(kim.requiredId, lee.requiredId),
                )

                Then("카테고리당 여러 명이 저장된다") {
                    val fire =
                        siteSensorManagerService
                            .findBySite(site.requiredId)
                            .single { it.sensorType == SensorType.FIRE }
                    fire.managers.map { it.name } shouldContainExactlyInAnyOrder listOf("김담당", "이담당")
                    fire.managers.first().department shouldBe "안전관리"
                }
            }

            When("같은 카테고리를 다시 지정") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))
                val lee = userRepository.save(user("이담당"))
                val park = userRepository.save(user("박담당"))

                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(kim.requiredId, lee.requiredId))
                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(park.requiredId))

                Then("이전 지정은 남지 않고 전체 교체된다") {
                    siteSensorManagerService
                        .findBySite(site.requiredId)
                        .single { it.sensorType == SensorType.FIRE }
                        .managers
                        .map { it.name } shouldContainExactly listOf("박담당")
                }
            }

            When("빈 목록으로 지정") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))
                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(kim.requiredId))

                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, emptyList())

                Then("담당자가 모두 해제된다") {
                    siteSensorManagerService
                        .findBySite(site.requiredId)
                        .single { it.sensorType == SensorType.FIRE }
                        .managers
                        .shouldHaveSize(0)
                }
            }

            When("같은 사용자를 중복으로 보냄") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))

                siteSensorManagerService.replace(
                    site.requiredId,
                    SensorType.FIRE,
                    listOf(kim.requiredId, kim.requiredId),
                )

                Then("한 번만 저장된다") {
                    siteSensorManagerRepository.findAllBySiteIdAndSensorType(site.requiredId, SensorType.FIRE) shouldHaveSize 1
                }
            }

            When("다른 카테고리에 지정") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))
                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(kim.requiredId))

                siteSensorManagerService.replace(site.requiredId, SensorType.ODOR_MONITOR, listOf(kim.requiredId))

                Then("카테고리별로 따로 관리된다") {
                    val all = siteSensorManagerService.findBySite(site.requiredId)
                    all.single { it.sensorType == SensorType.FIRE }.managers shouldHaveSize 1
                    all.single { it.sensorType == SensorType.ODOR_MONITOR }.managers shouldHaveSize 1
                    all shouldHaveSize SensorType.entries.size
                }
            }

            When("없는 사용자를 지정") {
                val site = siteRepository.save(SiteFixture.create())

                Then("예외를 던지고 아무것도 저장하지 않는다") {
                    shouldThrowExactly<CustomException> {
                        siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(999_999L))
                    }
                    siteSensorManagerRepository.findAll() shouldHaveSize 0
                }
            }

            When("없는 현장에 지정") {
                Then("예외를 던진다") {
                    shouldThrowExactly<CustomException> {
                        siteSensorManagerService.replace(999_999L, SensorType.FIRE, emptyList())
                    }
                }
            }
        }

        Given("이벤트 알림 대상 조회") {
            When("전화번호가 없는 담당자가 섞여 있음") {
                val site = siteRepository.save(SiteFixture.create())
                val withPhone = userRepository.save(user("김담당", phoneNumber = "010-1111-2222"))
                val withoutPhone = userRepository.save(user("이담당", phoneNumber = null))
                val blankPhone = userRepository.save(user("박담당", phoneNumber = "  "))
                siteSensorManagerService.replace(
                    site.requiredId,
                    SensorType.FIRE,
                    listOf(withPhone.requiredId, withoutPhone.requiredId, blankPhone.requiredId),
                )

                Then("번호가 있는 담당자만 대상이 된다") {
                    siteSensorManagerService.findManagerPhoneNumbers(site.requiredId, SensorType.FIRE) shouldContainExactly
                        listOf("010-1111-2222")
                }
            }
        }

        Given("현장을 삭제") {
            When("담당자가 지정된 현장을 지움") {
                val site = siteRepository.save(SiteFixture.create())
                val kim = userRepository.save(user("김담당"))
                siteSensorManagerService.replace(site.requiredId, SensorType.FIRE, listOf(kim.requiredId))

                siteService.delete(site.requiredId)

                Then("담당자 매핑도 함께 정리된다") {
                    siteSensorManagerRepository.findAll() shouldHaveSize 0
                }
            }
        }
    })
