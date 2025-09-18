package com.pluxity.aiot.domain.account.group.dto

import com.pluxity.aiot.domain.account.AccountRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AccountGroupRequest(
    @field:Schema(
        description = "그룹 이름",
        example = "group",
    )
    @field:NotBlank(message = "그룹 이름은 필수 입니다.")
    @field:Size(
        max = 16,
        message = "그룹 이름은 최대 16자까지 입력 가능합니다.",
    )
    val name: String,
    @field:Schema(
        description = "그룹 권한",
    )
    @field:NotBlank(message = "그룹 권한은 필수 입니다.")
    val role: AccountRole,
    @field:Schema(
        description = "메뉴 권한",
        example = "/admin/account",
    )
    val menuPaths: List<String>,
    @field:Schema(
        description = "접근 권한",
        example = "1",
    )
    val boundaryIds: List<Long>,
)
