package com.pluxity.aiot.system.device.type

import com.pluxity.aiot.system.device.profile.DeviceProfileType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class DeviceType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "object_id", nullable = false, unique = true)
    var objectId: String,
    @Column(name = "description", nullable = false)
    var description: String? = null,
    @Column(name = "version", nullable = false)
    var version: String? = null,
) {
    @OneToMany(mappedBy = "deviceType", cascade = [CascadeType.ALL], orphanRemoval = true)
    var deviceProfileTypes: MutableSet<DeviceProfileType> = mutableSetOf()
}
