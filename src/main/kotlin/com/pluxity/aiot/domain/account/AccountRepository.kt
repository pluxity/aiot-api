package com.pluxity.aiot.domain.account

import com.pluxity.aiot.domain.account.group.AccountGroup
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
    fun existsByUserid(userid: String): Boolean

    fun findAccountByUserid(userid: String): Account?

    fun findByUserid(username: String): Account?

    fun existsByAccountGroup(accountGroup: AccountGroup): Boolean
}
