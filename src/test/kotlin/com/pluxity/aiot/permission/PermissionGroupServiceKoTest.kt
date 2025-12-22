package com.pluxity.aiot.permission

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.permission.dto.PermissionGroupUpdateRequest
import com.pluxity.aiot.permission.dto.PermissionRequest
import com.pluxity.aiot.permission.dto.dummyPermissionGroupCreateRequest
import com.pluxity.aiot.permission.dto.dummyPermissionGroupUpdateRequest
import com.pluxity.aiot.permission.entity.dummyPermission
import com.pluxity.aiot.permission.entity.dummyPermissionGroup
import com.pluxity.aiot.user.repository.RolePermissionRepository
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.repository.findByIdOrNull

class PermissionGroupServiceKoTest :
    BehaviorSpec({
        val permissionGroupRepository: PermissionGroupRepository = mockk()
        val permissionRepository: PermissionRepository = mockk()
        val rolePermissionRepository: RolePermissionRepository = mockk()

        val permissionGroupService =
            PermissionGroupService(
                permissionGroupRepository,
                permissionRepository,
                rolePermissionRepository,
            )

        Given("Permission Group 생성을 진행할 때") {
            When("이미 존재하는 이름으로 생성 요청") {
                val name = "duplicate"
                val request =
                    dummyPermissionGroupCreateRequest(
                        name = name,
                    )

                every { permissionGroupRepository.existsByName(name) } returns true

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.create(request)
                    }

                Then("DUPLICATE_PERMISSION_GROUP_NAME 예외 발생") {
                    exception.message shouldBe ErrorCode.DUPLICATE_PERMISSION_GROUP_NAME.getMessage().format(name)
                }
            }

            When("유효하지 않은 resourceType으로 생성 요청") {
                val request =
                    dummyPermissionGroupCreateRequest(
                        permissions = listOf(PermissionRequest(resourceType = "INVALID", resourceIds = listOf("1"))),
                    )

                every { permissionGroupRepository.existsByName(any()) } returns false

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.create(request)
                    }

                Then("INVALID_RESOURCE_TYPE 예외 발생") {
                    exception.message shouldBe ErrorCode.INVALID_RESOURCE_TYPE.getMessage().format("Resource type: INVALID")
                }
            }

            When("동일 resourceType에 중복 resourceIds로 생성 요청") {
                val request =
                    dummyPermissionGroupCreateRequest(
                        permissions = listOf(PermissionRequest(resourceType = "SITE", resourceIds = listOf("1", "1"))),
                    )

                every { permissionGroupRepository.existsByName(any()) } returns false

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.create(request)
                    }

                Then("DUPLICATE_RESOURCE_ID 예외 발생") {
                    exception.message shouldBe ErrorCode.DUPLICATE_RESOURCE_ID.getMessage()
                }
            }

            When("유효한 요청으로 생성 요청") {
                val request =
                    dummyPermissionGroupCreateRequest(
                        permissions = listOf(PermissionRequest(resourceType = "SITE", resourceIds = listOf("1", "2"))),
                    )

                every { permissionGroupRepository.existsByName(any()) } returns false

                val savedId = 10L
                val savedGroupSlot = slot<PermissionGroup>()
                every { permissionGroupRepository.save(capture(savedGroupSlot)) } returns
                    dummyPermissionGroup(
                        id = savedId,
                        name = request.name,
                        description = request.description,
                    )

                val result = permissionGroupService.create(request)

                Then("PermissionGroup을 저장하고 id를 반환") {
                    result shouldBe savedId

                    savedGroupSlot.captured.name shouldBe request.name
                    savedGroupSlot.captured.description shouldBe request.description

                    savedGroupSlot.captured.permissions.map { it.resourceName } shouldContainExactlyInAnyOrder
                        listOf(
                            "SITE",
                            "SITE",
                        )
                    savedGroupSlot.captured.permissions.map { it.resourceId } shouldContainExactlyInAnyOrder
                        listOf(
                            "1",
                            "2",
                        )
                    savedGroupSlot.captured.permissions.all { it.permissionGroup == savedGroupSlot.captured } shouldBe true
                }
            }
        }

        Given("Permission Group 조회를 진행할 때") {
            When("유효한 id로 단건 조회") {
                val group =
                    dummyPermissionGroup(
                        id = 1L,
                        name = "group",
                        description = "desc",
                        permissions =
                            listOf(
                                dummyPermission(id = 1L, resourceId = "1"),
                                dummyPermission(id = 2L, resourceId = "2"),
                            ),
                    )

                every { permissionGroupRepository.findByIdOrNull(1L) } returns group

                val result = permissionGroupService.findById(1L)

                Then("PermissionGroupResponse 반환") {
                    result.id shouldBe 1L
                    result.name shouldBe "group"
                    result.description shouldBe "desc"
                    result.permissions.single().resourceType shouldBe "SITE"
                    result.permissions.single().resourceIds shouldContainExactlyInAnyOrder listOf("1", "2")
                }
            }

            When("없는 id로 단건 조회") {
                every { permissionGroupRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.findById(999L)
                    }

                Then("NOT_FOUND_PERMISSION_GROUP 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_PERMISSION_GROUP.getMessage().format(999L)
                }
            }

            When("전체 조회") {
                val group1 = dummyPermissionGroup(id = 1L, name = "g1")
                val group2 = dummyPermissionGroup(id = 2L, name = "g2", description = "d")

                every { permissionGroupRepository.findAll() } returns listOf(group1, group2)

                val result = permissionGroupService.findAll()

                Then("목록 반환") {
                    result.map { it.id } shouldContainExactlyInAnyOrder listOf(1L, 2L)
                }
            }
        }

        Given("Permission Group 수정을 진행할 때") {
            When("없는 id로 수정 요청") {
                val request = dummyPermissionGroupUpdateRequest()
                every { permissionGroupRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.update(999L, request)
                    }

                Then("NOT_FOUND_PERMISSION_GROUP 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_PERMISSION_GROUP.getMessage().format(999L)
                }
            }

            When("이름이 중복되는 이름으로 변경 요청") {
                val request = dummyPermissionGroupUpdateRequest(name = "dup")
                val group = dummyPermissionGroup(id = 1L, name = "old", description = null)

                every { permissionGroupRepository.findByIdOrNull(1L) } returns group
                every { permissionGroupRepository.existsByNameAndIdNot("dup", 1L) } returns true

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.update(1L, request)
                    }

                Then("DUPLICATE_PERMISSION_GROUP_NAME 예외 발생") {
                    exception.message shouldBe ErrorCode.DUPLICATE_PERMISSION_GROUP_NAME.getMessage().format("dup")
                }
            }

            When("권한을 삭제/추가하는 수정 요청") {
                val group =
                    dummyPermissionGroup(
                        id = 1L,
                        name = "old",
                        description = "oldDesc",
                        permissions =
                            listOf(
                                dummyPermission(id = 1L, resourceId = "1"),
                                dummyPermission(id = 2L, resourceId = "2"),
                            ),
                    )
                val request =
                    PermissionGroupUpdateRequest(
                        name = "new",
                        description = "newDesc",
                        permissions = listOf(PermissionRequest(resourceType = "SITE", resourceIds = listOf("2", "3"))),
                    )

                every { permissionGroupRepository.findByIdOrNull(1L) } returns group
                every { permissionGroupRepository.existsByNameAndIdNot("new", 1L) } returns false
                val deletedPermissionSlot = slot<Permission>()
                every { permissionRepository.delete(capture(deletedPermissionSlot)) } just runs

                permissionGroupService.update(1L, request)

                Then("이름/설명/권한이 반영되고 제거된 권한은 삭제 처리") {
                    group.name shouldBe "new"
                    group.description shouldBe "newDesc"
                    group.permissions.map { it.resourceId } shouldContainExactlyInAnyOrder listOf("2", "3")

                    verify(exactly = 1) { permissionRepository.delete(any()) }
                    deletedPermissionSlot.captured.resourceId shouldBe "1"
                }
            }
        }

        Given("Permission Group 삭제를 진행할 때") {
            When("유효한 id로 삭제 요청") {
                val group =
                    dummyPermissionGroup(
                        id = 1L,
                        name = "g",
                        description = null,
                        permissions = listOf(dummyPermission(id = 1L, resourceId = "1")),
                    )

                every { permissionGroupRepository.findByIdOrNull(1L) } returns group
                every { rolePermissionRepository.deleteAllByPermissionGroup(group) } just runs
                every { permissionRepository.deleteAll(any<Iterable<Permission>>()) } just runs
                every { permissionGroupRepository.delete(group) } just runs

                permissionGroupService.delete(1L)

                Then("연관 데이터 삭제 후 PermissionGroup 삭제") {
                    verify(exactly = 1) { rolePermissionRepository.deleteAllByPermissionGroup(group) }
                    verify(exactly = 1) { permissionRepository.deleteAll(group.permissions) }
                    verify(exactly = 1) { permissionGroupRepository.delete(group) }
                }
            }

            When("없는 id로 삭제 요청") {
                every { permissionGroupRepository.findByIdOrNull(999L) } returns null

                val exception =
                    shouldThrowExactly<CustomException> {
                        permissionGroupService.delete(999L)
                    }

                Then("NOT_FOUND_PERMISSION_GROUP 예외 발생") {
                    exception.message shouldBe ErrorCode.NOT_FOUND_PERMISSION_GROUP.getMessage().format(999L)
                }
            }
        }
    })
