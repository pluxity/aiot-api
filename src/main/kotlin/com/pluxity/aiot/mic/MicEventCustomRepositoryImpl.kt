package com.pluxity.aiot.mic

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.global.utils.findPageNotNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class MicEventCustomRepositoryImpl(
    private val kotlinJdslJpqlExecutor: KotlinJdslJpqlExecutor,
) : MicEventCustomRepository {
    override fun findAllByFilter(
        pageable: Pageable,
        micId: String?,
        from: String?,
        to: String?,
    ): Page<MicEvent> =
        kotlinJdslJpqlExecutor
            .findPageNotNull(pageable) {
                select(entity(MicEvent::class))
                    .from(entity(MicEvent::class))
                    .where(
                        and(
                            micId?.let { path(MicEvent::micId).equal(it) },
                            from?.let {
                                path(MicEvent::occurredAt).greaterThanOrEqualTo(
                                    DateTimeUtils.parseCompactDateTime(it),
                                )
                            },
                            to?.let {
                                path(MicEvent::occurredAt).lessThanOrEqualTo(
                                    DateTimeUtils.parseCompactDateTime(it),
                                )
                            },
                        ),
                    ).orderBy(
                        path(MicEvent::occurredAt).desc(),
                    )
            }
}
