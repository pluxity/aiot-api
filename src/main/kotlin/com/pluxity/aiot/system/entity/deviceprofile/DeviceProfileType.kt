package com.pluxity.aiot.system.entity.deviceprofile

import com.pluxity.aiot.system.entity.DeviceType
import com.pluxity.aiot.system.entity.EventSetting
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
    var deviceType: DeviceType? = null,
) {
    @OneToMany(mappedBy = "deviceProfileType", cascade = [CascadeType.ALL], orphanRemoval = true)
    val eventSettings: MutableSet<EventSetting> = mutableSetOf()

    fun addEventSetting(eventSetting: EventSetting) {
        this.eventSettings.add(eventSetting)
    }

    fun getEventSetting(): EventSetting? = eventSettings.firstOrNull()
}
