package com.pluxity.aiot.system.event.setting

import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.event.condition.EventCondition
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy

@Entity
class EventSetting(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_profile_type_id")
    var deviceProfileType: DeviceProfileType? = null,
    var eventEnabled: Boolean = false,
    // 기간 설정 관련 필드 추가
    var isPeriodic: Boolean = false,
    var isOriginal: Boolean = false,
    @ElementCollection
    @CollectionTable(name = "event_setting_months", joinColumns = [JoinColumn(name = "event_setting_id")])
    @Column(name = "month")
    @OrderBy("month ASC")
    var months: MutableSet<Int>? = mutableSetOf(),
) {
    @OneToMany(mappedBy = "eventSetting", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("id ASC")
    var conditions: MutableSet<EventCondition> = mutableSetOf()

    fun updateEventEnabled(eventEnabled: Boolean) {
        this.eventEnabled = eventEnabled
    }

    fun updatePeriodic(isPeriodic: Boolean) {
        this.isPeriodic = isPeriodic
    }

    fun updateMonths(months: List<Int>?) {
        this.months?.clear()
        months?.let { this.months?.addAll(months) }
    }

    fun addCondition(condition: EventCondition) {
        this.conditions.add(condition)
        condition.addEventSetting(this)
    }
}
