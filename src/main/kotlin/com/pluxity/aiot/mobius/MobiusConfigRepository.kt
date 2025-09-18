package com.pluxity.aiot.mobius

import org.springframework.data.jpa.repository.JpaRepository

interface MobiusConfigRepository : JpaRepository<MobiusConfig, Long> {
    fun findTopByOrderByCreatedAtDesc(): MobiusConfig?
}
