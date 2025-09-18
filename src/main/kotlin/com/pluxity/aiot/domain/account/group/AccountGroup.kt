package com.pluxity.aiot.domain.account.group

import com.pluxity.aiot.domain.account.Account
import com.pluxity.aiot.domain.account.AccountRole
import com.pluxity.aiot.domain.account.group.dto.AccountGroupRequest
import com.pluxity.aiot.domain.boundary.Boundary
import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "account_group")
class AccountGroup(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long? = null,
    @Column(nullable = false, unique = true, length = 16)
    var name: String,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var role: AccountRole,
) : BaseEntity() {
    @OneToMany(mappedBy = "accountGroup", cascade = [CascadeType.ALL], orphanRemoval = true)
    val accounts: MutableSet<Account> = mutableSetOf()

    @ElementCollection
    @CollectionTable(name = "account_group_menu", joinColumns = [JoinColumn(name = "account_group_id")])
    @Column(name = "menu_path")
    val accessibleMenus: MutableSet<String> = mutableSetOf()

    @ManyToMany
    @JoinTable(
        name = "account_group_boundary",
        joinColumns = [JoinColumn(name = "account_group_id")],
        inverseJoinColumns = [JoinColumn(name = "boundary_id")],
    )
    val boundaries: MutableSet<Boundary> = mutableSetOf()

    fun update(dto: AccountGroupRequest) {
        this.name = dto.name
        this.role = dto.role
    }

    fun updateAccessibleBoundaries(drawings: List<Boundary>) {
        this.boundaries.clear()
        this.boundaries.addAll(drawings)
    }

    fun updateAccessibleMenus(menuPaths: List<String>) {
        this.accessibleMenus.clear()
        this.accessibleMenus.addAll(menuPaths)
    }
}
