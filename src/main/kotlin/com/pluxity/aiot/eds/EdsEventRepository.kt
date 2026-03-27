package com.pluxity.aiot.eds

import org.springframework.data.jpa.repository.JpaRepository

interface EdsEventRepository : JpaRepository<EdsEvent, Long> {
    fun findByEventId(eventId: Int): EdsEvent?
}
