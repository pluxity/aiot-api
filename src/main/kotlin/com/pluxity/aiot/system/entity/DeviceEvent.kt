package com.pluxity.aiot.system.entity

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
import jakarta.persistence.Table

@Entity
@Table(name = "device_event")
class DeviceEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "image_url")
    var imageUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "device_level")
    var deviceLevel: DeviceLevel?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    var deviceType: DeviceType,
) {
    fun updateDeviceType(deviceType: DeviceType) {
        this.deviceType.deviceEvents.remove(this)
        this.deviceType = deviceType
        if (!deviceType.deviceEvents.contains(this)) {
            deviceType.deviceEvents.add(this)
        }
    }

    fun update(
        name: String,
        imageUrl: String?,
        deviceLevel: DeviceLevel,
    ) {
        this.name = name
        this.imageUrl = imageUrl
        this.deviceLevel = deviceLevel
    }

    fun changeImageUrl(imageUrl: String?) {
        this.imageUrl = imageUrl
    }

    enum class DeviceLevel {
        NORMAL,
        WARNING,
        CAUTION,
        DANGER,
        DISCONNECTED,
    }
}
