package com.pluxity.aiot.domain.account

import com.pluxity.aiot.domain.account.group.AccountGroup
import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "account")
data class Account(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    var accountGroup: AccountGroup,
    @Column(nullable = false, unique = true, length = 16)
    val userid: String,
    @Column(nullable = false)
    var username: String,
    @Column(nullable = false)
    var password: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var role: AccountRole,
    @Column(name = "expiration_time")
    var expirationTime: Long? = null,
    @Column(name = "last_login_time")
    var lastLoginTime: LocalDateTime? = null,
) : BaseEntity() {
    //    fun update(dto: AccountRequestDto) {
//        dto.username?.takeIf { it.isNotBlank() }?.let { username = it }
//
//        dto.accountGroup?.let { newGroup ->
//            if (newGroup != accountGroup) {
//                accountGroup.removeAccount(this)
//                accountGroup = newGroup
//                newGroup.addAccount(this)
//            }
//        }
//
//        dto.expirationTime?.let { expirationTime = it }
//    }

    fun changeLastLoginTime() {
        this.lastLoginTime = LocalDateTime.now()
    }

    fun changeExpirationTime(expirationTime: Long?) {
        this.expirationTime = expirationTime
    }

    val isExpired: Boolean
        get() {
            if (this.lastLoginTime == null || this.expirationTime == null) {
                return false
            }

            val expiryDateTime = this.lastLoginTime!!.plusMinutes(this.expirationTime!!)
            return LocalDateTime.now().isAfter(expiryDateTime)
        }

//    fun toDto(): AccountDto =
//        AccountDto(
//            id = id,
//            accountGroupDto = accountGroup.toDto(),
//            userid = userid,
//            username = username,
//            password = password,
//            role = role,
//            lastLoginTime = lastLoginTime,
//            expirationTime = expirationTime,
//            updDt = updDt
//        )
}
