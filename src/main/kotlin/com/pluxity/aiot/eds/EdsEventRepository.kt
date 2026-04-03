package com.pluxity.aiot.eds

import org.springframework.data.jpa.repository.JpaRepository

interface EdsEventRepository :
    JpaRepository<EdsEvent, Long>,
    EdsEventCustomRepository {
    fun findByEventIdAndEventStatusNot(
        eventId: Int,
        eventStatus: EdsEventStatus,
    ): EdsEvent?
}
