package com.pluxity.aiot.announcement

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class LlmMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: Site,
    @Column(nullable = false)
    val yesterdayAvgTemp: Double,
    @Column(nullable = false)
    val todayAvgTemp: Double,
    @Column(length = 500, nullable = false)
    val prompt: String,
    @Column(length = 1000, nullable = false)
    var message: String,
) : BaseEntity() {
    val requiredId: Long
        get() = checkNotNull(id) { "LlmMessage is not persisted yet" }
}
