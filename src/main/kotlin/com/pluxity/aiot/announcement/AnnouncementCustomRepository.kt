package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.SearchRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AnnouncementCustomRepository {
    fun findAllBySearchRequest(
        pageable: Pageable,
        request: SearchRequest,
    ): Page<Announcement>
}
