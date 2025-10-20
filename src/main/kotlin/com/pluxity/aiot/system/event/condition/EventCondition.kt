package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.system.device.event.DeviceEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class EventCondition(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_event_id")
    val deviceEvent: DeviceEvent,
    @Column(name = "is_active")
    var isActivate: Boolean = false,
    var needControl: Boolean = false,
    var isBoolean: Boolean = false,
    @Column(name = "min_value")
    var minValue: String? = null,
    @Column(name = "max_value")
    var maxValue: String? = null,
    var notificationEnabled: Boolean = false,
    @Column(name = "notification_interval_minutes")
    var notificationIntervalMinutes: Int = 0,
    @Column(name = "condition_order")
    var order: Int? = null,
) {
    init {
        if (order == null) {
            this.order = getDefaultOrderByDeviceLevel()
        }
    }

    fun update(
        isActivate: Boolean,
        needControl: Boolean,
        isBoolean: Boolean,
        minValue: String?,
        maxValue: String?,
        notificationEnabled: Boolean,
        notificationIntervalMinutes: Int?,
        order: Int?,
    ) {
        this.isActivate = isActivate
        this.needControl = needControl
        this.isBoolean = isBoolean
        this.minValue = minValue
        this.maxValue = maxValue
        this.notificationEnabled = notificationEnabled
        this.notificationIntervalMinutes = notificationIntervalMinutes ?: 0
        this.order = order ?: this.order
    }

    private fun getDefaultOrderByDeviceLevel(): Int {
        if (this.deviceEvent.deviceLevel == null) {
            return 0
        }

        return when (this.deviceEvent.deviceLevel) {
            DeviceEvent.DeviceLevel.NORMAL -> 1
            DeviceEvent.DeviceLevel.WARNING -> 2
            DeviceEvent.DeviceLevel.CAUTION -> 3
            DeviceEvent.DeviceLevel.DANGER -> 4
            DeviceEvent.DeviceLevel.DISCONNECTED -> -1
            else -> 0
        }
    }

}
