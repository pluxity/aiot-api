package com.pluxity.aiot.system.device.profile

import com.pluxity.aiot.fixture.DeviceProfileFixture
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.profile.dto.DeviceProfileRequest
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
class DeviceProfileServiceTest(
    private val deviceProfileService: DeviceProfileService,
    private val deviceProfileRepository: DeviceProfileRepository,
) : BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf
        extension(SpringExtension)

        Given("DeviceProfile 생성 기능") {
            val request =
                DeviceProfileRequest(
                    fieldKey = "temperature",
                    description = "Temperature sensor",
                    fieldUnit = "°C",
                    fieldType = DeviceProfile.FieldType.Float,
                )
            When("유효한 데이터로 프로필을 생성하면") {

                val createdId = deviceProfileService.create(request)

                Then("성공적으로 생성되고 ID가 반환된다") {
                    createdId shouldNotBe null
                    val saved = deviceProfileRepository.findById(createdId).get()
                    saved.fieldKey shouldBe "temperature"
                    saved.description shouldBe "Temperature sensor"
                    saved.fieldUnit shouldBe "°C"
                    saved.fieldType shouldBe DeviceProfile.FieldType.Float
                }
            }

            When("이미 존재하는 fieldKey로 프로필을 생성하면") {
                val request =
                    DeviceProfileRequest(
                        fieldKey = "temperature_dup",
                        description = "Temperature sensor",
                        fieldUnit = "°C",
                        fieldType = DeviceProfile.FieldType.Float,
                    )
                deviceProfileService.create(request)

                val exception =
                    shouldThrow<CustomException> {
                        deviceProfileService.create(request)
                    }

                Then("중복 예외(DUPLICATE_DEVICE_PROFILE_KEY)가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.DUPLICATE_DEVICE_PROFILE_KEY
                }
            }
        }

        Given("DeviceProfile 조회 기능") {
            When("전체 프로필을 조회하면") {
                // 사전 조건: 데이터 준비
                deviceProfileRepository.deleteAll()
                deviceProfileRepository.saveAll(
                    listOf(
                        DeviceProfileFixture.create(fieldKey = "temp_find1"),
                        DeviceProfileFixture.create(fieldKey = "humid_find1"),
                        DeviceProfileFixture.create(fieldKey = "power_find1"),
                    ),
                )

                val result = deviceProfileService.findAll()

                Then("모든 프로필이 반환된다") {
                    result shouldHaveSize 3
                }
            }
        }

        Given("DeviceProfile 업데이트 기능") {
            When("유효한 데이터로 프로필을 업데이트하면") {
                val profile = DeviceProfileFixture.create(fieldKey = "temp_update1")
                val saved = deviceProfileRepository.save(profile)
                val updateRequest =
                    DeviceProfileRequest(
                        fieldKey = "temp_updated",
                        description = "Updated temperature sensor",
                        fieldUnit = "K",
                        fieldType = DeviceProfile.FieldType.Integer,
                    )

                deviceProfileService.update(saved.id!!, updateRequest)

                Then("성공적으로 업데이트된다") {
                    val updated = deviceProfileRepository.findById(saved.id!!).get()
                    updated.fieldKey shouldBe "temp_updated"
                    updated.description shouldBe "Updated temperature sensor"
                    updated.fieldUnit shouldBe "K"
                    updated.fieldType shouldBe DeviceProfile.FieldType.Integer
                }
            }

            When("다른 프로필과 중복되는 fieldKey로 업데이트하면") {
                // 사전 조건: 업데이트할 프로필과 중복될 프로필 생성
                val profileToUpdate = deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "temp_dup_update"))
                deviceProfileRepository.save(DeviceProfileFixture.create(fieldKey = "humid_dup_update"))
                val updateRequest =
                    DeviceProfileRequest(
                        fieldKey = "humid_dup_update",
                        description = "Updated",
                        fieldUnit = "°C",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val exception =
                    shouldThrow<CustomException> {
                        deviceProfileService.update(profileToUpdate.id!!, updateRequest)
                    }

                Then("중복 예외(DUPLICATE_DEVICE_PROFILE_KEY)가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.DUPLICATE_DEVICE_PROFILE_KEY
                }
            }

            When("존재하지 않는 ID로 업데이트를 시도하면") {
                val updateRequest =
                    DeviceProfileRequest(
                        fieldKey = "test",
                        description = "test",
                        fieldUnit = "test",
                        fieldType = DeviceProfile.FieldType.Float,
                    )

                val exception =
                    shouldThrow<CustomException> {
                        deviceProfileService.update(999999L, updateRequest)
                    }

                Then("프로필을 찾을 수 없다는 예외(NOT_FOUND_DEVICE_PROFILE)가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_PROFILE
                }
            }
        }

        Given("DeviceProfile 삭제 기능") {
            When("존재하는 프로필을 삭제하면") {
                val profile = DeviceProfileFixture.create(fieldKey = "temp_delete1")
                val saved = deviceProfileRepository.save(profile)

                deviceProfileService.delete(saved.id!!)

                Then("성공적으로 삭제된다") {
                    deviceProfileRepository.findById(saved.id!!).isPresent shouldBe false
                }
            }

            When("존재하지 않는 프로필을 삭제하면") {
                val exception =
                    shouldThrow<CustomException> {
                        deviceProfileService.delete(999999L)
                    }

                Then("프로필을 찾을 수 없다는 예외(NOT_FOUND_DEVICE_PROFILE)가 발생한다") {
                    exception.errorCode shouldBe ErrorCode.NOT_FOUND_DEVICE_PROFILE
                }
            }
        }
    })
