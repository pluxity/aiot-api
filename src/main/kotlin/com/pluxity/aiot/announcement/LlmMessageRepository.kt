package com.pluxity.aiot.announcement

import com.pluxity.aiot.site.Site
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface LlmMessageRepository : JpaRepository<LlmMessage, Long> {
    fun findAllBySite(site: Site): List<LlmMessage>

    fun findAllByCreatedAtBetween(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<LlmMessage>

    fun findAllBySiteAndCreatedAtBetween(
        site: Site,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<LlmMessage>
}
