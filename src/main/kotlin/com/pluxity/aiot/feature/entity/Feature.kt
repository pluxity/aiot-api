package com.pluxity.aiot.feature.entity

import com.pluxity.aiot.facility.Facility
import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.system.device.type.DeviceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.awt.Point
import java.time.LocalDateTime

@Entity
class Feature(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    var deviceType: DeviceType? = null,
    @Column(nullable = false, unique = true)
    var deviceId: String? = null,
    @Column(nullable = false)
    var objectId: String? = null,
    var name: String? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    @Column(columnDefinition = "geometry(Point, 4326)")
    var coord: Point? = null,
    @Column
    var batteryLevel: Int? = null,
    @Column(length = 50)
    var eventStatus: String? = "NORMAL",
    var isActive: Boolean? = true,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    var facility: Facility? = null,
    @Column
    private var subscriptionTime: LocalDateTime? = null,
) : BaseEntity() {
    fun changeDeviceType(deviceType: DeviceType?) {
        this.deviceType = deviceType
    }
}
