package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.AnnouncementResponse
import com.pluxity.aiot.announcement.dto.BroadcastRequest
import com.pluxity.aiot.announcement.dto.SearchRequest
import com.pluxity.aiot.announcement.dto.toAnnouncementResponse
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.site.Site
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

        val page =
            announcementRepository.findPage(pageable) {
                select(entity(Announcement::class))
                    .from(
                        entity(Announcement::class),
                        join(Announcement::site),
                    ).where(
                        and(
                            request.siteId?.let { path(Site::id).eq(it) },
                            request.from?.let {
                                path(Announcement::createdAt).greaterThanOrEqualTo(
                                    DateTimeUtils
                                        .parseCompactDate(it)
                                        .atStartOfDay(),
                                )
                            },
                            request.to?.let {
                                path(Announcement::createdAt).lessThanOrEqualTo(
                                    DateTimeUtils
                                        .parseCompactDate(it)
                                        .atTime(23, 59, 59, 999_999_999),
                                )
                            },
                            request.userId?.let { path(Announcement::createdBy).eq(it) },
                        ),
                    ).orderBy(path(Announcement::id).desc())
            }

        return page.toPageResponse { it.toAnnouncementResponse() }
    }
}
