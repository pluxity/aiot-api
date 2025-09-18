package com.pluxity.aiot.system.entity.mobius

import org.springframework.data.jpa.repository.JpaRepository

interface MobiusConfigRepository : JpaRepository<MobiusConfig, Long> {
    fun findTopByOrderByCreatedAtDesc(): MobiusConfig?
}
