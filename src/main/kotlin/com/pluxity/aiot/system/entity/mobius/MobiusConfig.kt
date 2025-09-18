package com.pluxity.aiot.system.entity.mobius

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class MobiusConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'http://203.253.128.181:11000/Mobius/sawwave'")
    var url: String,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
