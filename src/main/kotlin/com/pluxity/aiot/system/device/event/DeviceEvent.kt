package com.pluxity.aiot.system.device.event

import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.event.condition.EventCondition
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "device_event")
class DeviceEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "name", nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "device_level")
    var deviceLevel: DeviceLevel?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    var deviceType: DeviceType? = null,
) {
    @OneToMany(mappedBy = "deviceEvent", cascade = [CascadeType.ALL], orphanRemoval = true)
    val eventConditions: MutableSet<EventCondition> = mutableSetOf()

    fun updateDeviceType(deviceType: DeviceType) {
        this.deviceType?.deviceEvents?.remove(this)
        this.deviceType = deviceType
        if (!deviceType.deviceEvents.contains(this)) {
            deviceType.deviceEvents.add(this)
        }
    }

    fun update(
        name: String,
        deviceLevel: DeviceLevel,
    ) {
        this.name = name
        this.deviceLevel = deviceLevel
    }

    enum class DeviceLevel {
        NORMAL,
        WARNING,
        CAUTION,
        DANGER,
        DISCONNECTED,
    }
}
