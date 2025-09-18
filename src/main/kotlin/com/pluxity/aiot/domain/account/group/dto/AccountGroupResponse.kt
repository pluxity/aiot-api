package com.pluxity.aiot.domain.account.group.dto

import com.pluxity.aiot.domain.account.AccountRole
import com.pluxity.aiot.domain.account.group.AccountGroup
import java.time.LocalDateTime

data class AccountGroupResponse(
    val id: Long,
    val updDt: LocalDateTime,
    val updUser: String,
    val name: String,
    val role: AccountRole,
    val accessibleMenus: List<String>,
)

fun AccountGroup.toAccountGroupResponse(): AccountGroupResponse =
    AccountGroupResponse(
        id = this.id!!,
        name = this.name,
        role = this.role,
        updDt = this.updatedAt,
        updUser = this.updatedBy!!,
        accessibleMenus = this.accessibleMenus.toList(),
    )
