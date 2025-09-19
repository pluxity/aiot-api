package com.pluxity.aiot.system.event.condition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventConditionRepository : JpaRepository<EventCondition, Long> {
    @Modifying
    @Query("DELETE FROM EventCondition ec WHERE ec.deviceEvent.id IN :eventIds")
    fun deleteAllByDeviceEventIdIn(
        @Param("eventIds") eventIds: Set<Long>,
    ): Int
}
