package com.pluxity.aiot.announcement

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class LlmMessage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val yesterdayAvgTemp: Double,
    @Column(nullable = false)
    val todayAvgTemp: Double,
    @Column(length = 500, nullable = false)
    val prompt: String,
    @Column(length = 1000, nullable = false)
    var message: String,
) : BaseEntity()
