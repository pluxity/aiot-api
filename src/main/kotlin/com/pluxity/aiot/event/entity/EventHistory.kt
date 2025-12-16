package com.pluxity.aiot.event.entity

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "event_history",
    indexes = [
        Index(name = "event_history_idx_event_status_id_desc", columnList = "status, id"),
    ],
)
class EventHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    var deviceId: String,
    @Column(nullable = false)
    var objectId: String,
    @Column(nullable = false)
    var sensorDescription: String,
    @Column(nullable = false)
    var fieldKey: String,
    @Column(nullable = false)
    var value: Double,
    @Column(nullable = false)
    var unit: String,
    @Column(nullable = false)
    var eventName: String,
    @Column(nullable = false)
    var occurredAt: LocalDateTime = LocalDateTime.now(),
    var minValue: Double? = null,
    var maxValue: Double? = null,
    @Enumerated(EnumType.STRING)
    var status: EventStatus = EventStatus.ACTIVE,
    var guideMessage: String? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    @Enumerated(EnumType.STRING)
    var level: ConditionLevel? = null,
) : BaseEntity() {
    fun changeStatus(status: EventStatus) {
        this.status = status
    }
}
