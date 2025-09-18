package com.pluxity.aiot.domain.account.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

class AccountUpdateRequest(
    @field:Schema(
        description = "그룹 아이디",
        example = "1",
    )
    @field:NotBlank(message = "그룹 아이디는 필수 입니다.")
    groupId: Long,
    @field:Schema(
        description = "사용자 이름",
        example = "username",
    )
    @field:NotBlank(message = "사용자 이름은 필수 입니다.")
    username: String,
    @field:Schema(
        description = "비밀번호",
        example = "password",
    )
    @field:NotBlank(message = "비밀번호는 필수 입니다.")
    password: String,
    @field:Schema(
        description = "접속유지시간",
        example = "30",
    )
    expirationTime: Long = 30L,
)
