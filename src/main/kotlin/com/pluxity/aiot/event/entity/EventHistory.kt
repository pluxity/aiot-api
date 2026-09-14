package com.pluxity.aiot.event.entity

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDateTime

/** 센서 조건 판정 원본. 조치 상태는 Incident가 가진다 */
@Entity
@Table(name = "event_history")
class EventHistory(
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
    var guideMessage: String? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    @Enumerated(EnumType.STRING)
    var level: ConditionLevel? = null,
) : BaseEntity()
