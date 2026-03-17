package com.pluxity.aiot.announcement

import com.pluxity.aiot.announcement.dto.SearchRequest
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.global.utils.findPageNotNull
import com.pluxity.aiot.site.Site
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AnnouncementCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : AnnouncementCustomRepository {
    override fun findAllBySearchRequest(
        pageable: Pageable,
        request: SearchRequest,
    ): Page<Announcement> =
        kotlinJdslJpqlExecutor
            .findPageNotNull(pageable) {
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
}
