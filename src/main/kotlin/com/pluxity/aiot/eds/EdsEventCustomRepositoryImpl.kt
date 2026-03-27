package com.pluxity.aiot.eds

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.global.utils.findPageNotNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class EdsEventCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : EdsEventCustomRepository {
    override fun findAllByFilter(
        pageable: Pageable,
        from: String?,
        to: String?,
    ): Page<EdsEvent> =
        kotlinJdslJpqlExecutor
            .findPageNotNull(pageable) {
                select(entity(EdsEvent::class))
                    .from(entity(EdsEvent::class))
                    .where(
                        and(
                            from?.let {
                                path(EdsEvent::createdAt).greaterThanOrEqualTo(
                                    DateTimeUtils.parseCompactDateTime(it),
                                )
                            },
                            to?.let {
                                path(EdsEvent::createdAt).lessThanOrEqualTo(
                                    DateTimeUtils.parseCompactDateTime(it),
                                )
                            },
                        ),
                    ).orderBy(
                        path(EdsEvent::createdAt).desc(),
                    )
            }
}
