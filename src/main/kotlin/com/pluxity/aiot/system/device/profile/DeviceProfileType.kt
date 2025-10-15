package com.pluxity.aiot.system.device.profile

import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.event.condition.EventCondition
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany

@Entity
class DeviceProfileType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_profile_id", nullable = false)
    var deviceProfile: DeviceProfile? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_type_id", nullable = false)
    var deviceType: DeviceType,
) {
    @OneToMany(mappedBy = "deviceProfileType", cascade = [CascadeType.ALL], orphanRemoval = true)
    val conditions: MutableSet<EventCondition> = mutableSetOf()

    fun addCondition(condition: EventCondition) {
        this.conditions.add(condition)
        condition.deviceProfileType = this
    }
}
