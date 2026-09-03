package com.pluxity.aiot.sms

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SmsHistoryRepository : JpaRepository<SmsHistory, Long> {
    /**
     * 항상 같은 페이지만 보면 확정되지 않는 건에 막혀 뒤쪽 건이 영영 조회되지 않는다.
     * 끝내 확정되지 않는 건이 조회 예산을 영구히 소비하지 않도록 오래된 건은 제외한다.
     */
    @Query(
        """
        select h from SmsHistory h
        where h.clidx is not null and h.resultCode is null and h.createdAt >= :cutoff
        order by h.resultCheckedAt asc nulls first, h.createdAt asc
        """,
    )
    fun findPendingResults(
        @Param("cutoff") cutoff: LocalDateTime,
        pageable: Pageable,
    ): List<SmsHistory>
}
