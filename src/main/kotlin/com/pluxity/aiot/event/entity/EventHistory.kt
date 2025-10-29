package com.pluxity.aiot.event.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "event_history")
class EventHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var deviceId: String? = null,
    @Column(nullable = false)
    var objectId: String? = null,
    @Column(nullable = false)
    var sensorDescription: String? = null,
    @Column(nullable = false)
    var fieldKey: String? = null,
    @Column(nullable = false)
    var value: Double? = null,
    @Column(nullable = false)
    var unit: String? = null,
    @Column(nullable = false)
    var eventName: String? = null,
    @Column(nullable = false)
    var occurredAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var minValue: Double? = null,
    @Column
    var maxValue: Double? = null,
    @Column
    @Enumerated(EnumType.STRING)
    var actionResult: HistoryResult = HistoryResult.PENDING,
    @Column
    var guideMessage: String? = null,
) {
    fun changeActionResult(actionResult: HistoryResult) {
        this.actionResult = actionResult
    }
}
