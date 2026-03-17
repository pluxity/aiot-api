package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.AnnouncementResponse
import com.pluxity.aiot.announcement.dto.BroadcastRequest
import com.pluxity.aiot.announcement.dto.SearchRequest
import com.pluxity.aiot.announcement.dto.toAnnouncementResponse
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import com.pluxity.aiot.site.SiteService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val siteService: SiteService,
) {
    @Transactional
    fun broadcast(request: BroadcastRequest) {
        // TODO 송출 API 호출 필요
        val sites = siteService.findByIds(request.siteIds)
        val announcements =
            sites.map { site ->
                Announcement(
                    site = site,
                    message = request.message,
                )
            }
        announcementRepository.saveAll(announcements)
    }

    fun findAll(request: SearchRequest): PageResponse<AnnouncementResponse> {
        val pageable =
            PageRequest.of(
                request.page - 1,
                request.size,
                Sort.by(Sort.Direction.DESC, "id"),
            )

        val page = announcementRepository.findAllBySearchRequest(pageable, request)

        return page.toPageResponse { it.toAnnouncementResponse() }
    }
}
