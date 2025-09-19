package com.pluxity.aiot.system.event.setting

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventSettingHistoryRepository : JpaRepository<EventSettingHistory, Long> {
    @Query(
        """
        SELECT h FROM EventSettingHistory h
        WHERE h.eventSetting.id = :settingId
        ORDER BY h.updatedAt DESC
    """,
    )
    fun findAllByEventSettingIdOrderByUpdatedAtDesc(
        @Param("settingId") settingId: Long,
    ): List<EventSettingHistory>

    fun deleteAllByEventSettingId(eventSettingId: Long)
}
