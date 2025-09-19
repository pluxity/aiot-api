package com.pluxity.aiot.system.event.condition

import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.event.setting.EventSetting
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
    @Enumerated(EnumType.STRING)
    var operator: ConditionOperator? = null,
    @Column(name = "min_value")
    var minValue: Double? = null,
    @Column(name = "max_value")
    var maxValue: Double? = null,
    var value: String,
    var notificationEnabled: Boolean = false,
    var locationTrackingEnabled: Boolean = false,
    var soundEnabled: Boolean = false,
    var fireEffectEnabled: Boolean = false,
    @Enumerated(EnumType.STRING)
    var controlType: ControlType? = ControlType.AUTO,
    @Column(length = 500)
    var guideMessage: String? = null,
    @Column(name = "notification_interval_minutes")
    var notificationIntervalMinutes: Int? = null,
    @Column(name = "condition_order")
    var order: Int? = null,
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_setting_id")
    var eventSetting: EventSetting? = null

    fun addEventSetting(eventSetting: EventSetting?) {
        this.eventSetting?.conditions?.remove(this)
        this.eventSetting = eventSetting
        eventSetting?.conditions?.add(this)
    }

    fun update(
        value: String,
        operator: ConditionOperator?,
        notificationEnabled: Boolean,
        locationTrackingEnabled: Boolean,
        soundEnabled: Boolean,
        fireEffectEnabled: Boolean,
        controlType: ControlType?,
        guideMessage: String?,
        notificationIntervalMinutes: Int?,
        order: Int?,
    ) {
        this.value = value
        this.operator = operator
        this.notificationEnabled = notificationEnabled
        this.locationTrackingEnabled = locationTrackingEnabled
        this.soundEnabled = soundEnabled
        this.fireEffectEnabled = fireEffectEnabled
        this.controlType = controlType
        this.guideMessage = guideMessage
        this.notificationIntervalMinutes = if (notificationIntervalMinutes != null) notificationIntervalMinutes else 0
        this.order = order ?: this.order
    }

    fun changeMinMax(
        minValue: Double?,
        maxValue: Double?,
    ) {
        this.minValue = minValue
        this.maxValue = maxValue
    }

    fun isAutoResponseEnabled(): Boolean = this.controlType == ControlType.AUTO

    private fun getDefaultOrderByDeviceLevel(): Int {
        if (this.deviceEvent == null || this.deviceEvent.deviceLevel == null) {
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

    enum class ConditionOperator {
        EQUALS,
        BETWEEN,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
    }

    enum class ControlType(
        val description: String,
    ) {
        AUTO("자동제어"),
        MANUAL("수동제어"),
    }
}
