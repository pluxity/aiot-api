package com.pluxity.aiot.system.mobius

import org.springframework.data.jpa.repository.JpaRepository

interface MobiusConfigRepository : JpaRepository<MobiusConfig, Long> {
    fun findTopByOrderByCreatedAtDesc(): MobiusConfig?
}
