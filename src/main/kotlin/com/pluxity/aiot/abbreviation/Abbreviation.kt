package com.pluxity.aiot.abbreviation

import com.pluxity.aiot.abbreviation.dto.AbbreviationRequest
import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "abbreviation")
class Abbreviation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "type", nullable = false)
    var type: String,
    @Column(name = "abbreviation_key", nullable = false, unique = true)
    var abbreviationKey: String,
    @Column(name = "full_name", nullable = false)
    var fullName: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) : BaseEntity() {
    fun update(request: AbbreviationRequest) {
        this.type = request.type
        this.abbreviationKey = request.abbreviationKey
        this.fullName = request.fullName
        this.description = request.description
        this.isActive = request.isActive
    }
}
