package com.pluxity.aiot.action

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.event.entity.EventHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ActionHistoryRepository :
    JpaRepository<ActionHistory, Long>,
    KotlinJdslJpqlExecutor {
    fun findByEventHistory(eventHistory: EventHistory): List<ActionHistory>

    fun findByIdAndEventHistory(
        id: Long,
        eventHistory: EventHistory,
    ): ActionHistory?
}
