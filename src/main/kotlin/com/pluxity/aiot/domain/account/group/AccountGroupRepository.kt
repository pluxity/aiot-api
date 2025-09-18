package com.pluxity.aiot.domain.account.group

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.domain.account.AccountRole
import org.springframework.data.jpa.repository.JpaRepository

interface AccountGroupRepository :
    JpaRepository<AccountGroup, Long>,
    KotlinJdslJpqlExecutor {
    fun findByRole(role: AccountRole): AccountGroup?

    fun existsByNameAndIdNot(
        name: String,
        id: Long,
    ): Boolean

    fun existsByName(name: String): Boolean
}
