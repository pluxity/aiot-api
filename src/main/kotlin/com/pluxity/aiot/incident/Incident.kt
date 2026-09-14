package com.pluxity.aiot.incident

import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.entity.EventStatus
import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "incident",
    uniqueConstraints = [UniqueConstraint(name = "incident_uk_source", columnNames = ["source_type", "source_id"])],
    indexes = [
        Index(name = "incident_idx_status_id", columnList = "status, id"),
        Index(name = "incident_idx_site_occurred_at", columnList = "site_id, occurred_at"),
    ],
)
class Incident(
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    val sourceType: IncidentSourceType,
    @Column(name = "source_id", nullable = false)
    val sourceId: Long,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    var site: Site? = null,
    @Column(nullable = false)
    var deviceId: String,
    var deviceName: String? = null,
    @Column(nullable = false)
    var title: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var level: ConditionLevel,
    @Column(nullable = false)
    var occurredAt: LocalDateTime,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var guideMessage: String? = null,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.ACTIVE
        protected set

    var resolvedAt: LocalDateTime? = null
        protected set

    fun changeStatus(status: EventStatus) {
        this.status = status
        this.resolvedAt = if (status == EventStatus.RESOLVED) LocalDateTime.now() else null
    }
}
