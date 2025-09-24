package com.pluxity.aiot.alarm.repository

import com.pluxity.aiot.alarm.entity.EventHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventHistoryRepository :
    JpaRepository<EventHistory, Long>,
    EventHistoryRepositoryCustom {
    fun findByDeviceId(deviceId: String): List<EventHistory>

    @Query(
        value =
            "SELECT e FROM EventHistory e " +
                "WHERE e.deviceId = :deviceId " +
                "AND (:keyword IS NULL OR :keyword = '' OR e.sensorDescription LIKE :keyword) " +
                "AND e.occurredAt BETWEEN :startTime AND :endTime",
    )
    fun findByDeviceIdAndDateRange(
        @Param("deviceId") deviceId: String,
        @Param("keyword") keyword: String,
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime,
    ): List<EventHistory>

    fun findByDeviceIdOrderByOccurredAtDesc(deviceId: String): List<EventHistory>

    @Query("SELECT e FROM EventHistory e WHERE e.occurredAt >= :startTime")
    fun findEventsInLast24Hours(
        @Param("startTime") startTime: LocalDateTime,
    ): List<EventHistory>

    @Query("SELECT e FROM EventHistory e WHERE e.actionResult = 'MANUAL_PENDING'")
    fun findByActionResultIsManualPending(): List<EventHistory>

    @Query("SELECT e FROM EventHistory e WHERE e.actionResult LIKE 'AUTOMATIC%'")
    fun findByActionResultStartsWithAutomatic(): List<EventHistory>

    @Query("SELECT e FROM EventHistory e WHERE e.actionResult LIKE 'MANUAL%'")
    fun findByActionResultStartsWithManual(): List<EventHistory>

    @Query("SELECT e FROM EventHistory e WHERE e.actionResult LIKE :prefix%")
    fun findByActionResultStartsWith(
        @Param("prefix") prefix: String,
    ): List<EventHistory>
}
