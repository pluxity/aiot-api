package com.pluxity.aiot.system.event.setting

import com.pluxity.aiot.system.event.condition.EventCondition
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EventSettingRepository : JpaRepository<EventSetting, Long> {
    @EntityGraph(attributePaths = ["conditions", "conditions.deviceEvent", "deviceProfileType"])
    fun findWithConditionsById(id: Long): EventSetting?

    @EntityGraph(attributePaths = ["conditions", "conditions.deviceEvent", "deviceProfileType"])
    override fun findAll(): List<EventSetting>

    @EntityGraph(attributePaths = ["conditions", "conditions.deviceEvent", "deviceProfileType"])
    fun findAllByDeviceProfileTypeId(deviceProfileTypeId: Long): List<EventSetting>

    @EntityGraph(attributePaths = ["conditions", "conditions.deviceEvent", "deviceProfileType"])
    fun findByDeviceProfileTypeId(deviceProfileTypeId: Long): EventSetting?

    @Query("SELECT c FROM EventSetting e JOIN e.conditions c WHERE c.id = :conditionId")
    fun findConditionById(
        @Param("conditionId") conditionId: Long,
    ): EventCondition?
}
