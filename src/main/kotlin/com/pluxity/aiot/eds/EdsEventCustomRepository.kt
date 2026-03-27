package com.pluxity.aiot.eds

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EdsEventCustomRepository {
    fun findAllByFilter(
        pageable: Pageable,
        from: String?,
        to: String?,
    ): Page<EdsEvent>
}
