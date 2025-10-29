package com.pluxity.aiot.event.condition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional

interface EventConditionRepository : JpaRepository<EventCondition, Long> {
    fun findAllByObjectId(objectId: String): List<EventCondition>

    @Modifying
    @Transactional
    fun deleteAllByObjectId(objectId: String): Int
}
