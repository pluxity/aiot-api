package com.pluxity.aiot.mic

import org.springframework.data.jpa.repository.JpaRepository

interface MicEventRepository :
    JpaRepository<MicEvent, Long>,
    MicEventCustomRepository {
    fun existsByEventId(eventId: String): Boolean
}
