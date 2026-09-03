package com.pluxity.aiot.mic

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MicRepository : JpaRepository<Mic, Long> {
    @Query("select m from Mic m left join fetch m.site")
    fun findAllWithSite(): List<Mic>
}
