package com.pluxity.aiot.mic

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface MicEventCustomRepository {
    fun findAllByFilter(
        pageable: Pageable,
        micId: String?,
        from: String?,
        to: String?,
    ): Page<MicEvent>
}
