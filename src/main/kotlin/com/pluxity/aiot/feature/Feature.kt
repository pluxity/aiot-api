package com.pluxity.aiot.feature

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.system.device.type.DeviceType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
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
    var deviceId: String,
    @Column(nullable = false)
    var objectId: String,
    var name: String? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    @Column(columnDefinition = "geometry(Point, 4326)")
    var geom: Point? = null,
    @Column
    var batteryLevel: Int? = null,
    @Column(length = 50)
    var eventStatus: String? = "NORMAL",
    var isActive: Boolean? = true,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    var site: Site? = null,
    @Column
    private var subscriptionTime: LocalDateTime? = null,
) : BaseEntity() {
    fun changeDeviceType(deviceType: DeviceType?) {
        this.deviceType = deviceType
    }

    fun updateDeviceType(deviceType: DeviceType) {
        this.deviceType = deviceType
    }

    fun updateActive(isActive: Boolean) {
        this.isActive = isActive
    }

    fun updateInfo(
        deviceType: DeviceType?,
        name: String,
        objectId: String,
    ) {
        this.deviceType = deviceType
        this.name = name
        this.objectId = objectId
    }

    fun updateStatusInfo(
        longitude: Double,
        latitude: Double,
        batteryLevel: Int?,
        site: Site?,
    ) {
        this.longitude = longitude
        this.latitude = latitude
        this.batteryLevel = batteryLevel
        // Point 객체 생성 (SRID 4326 사용)
        val gf = GeometryFactory(PrecisionModel(), 4326)
        this.geom = gf.createPoint(Coordinate(longitude, latitude))
        this.site = site
    }

    fun updateSubscriptionTime(subscriptionTime: LocalDateTime) {
        this.subscriptionTime = subscriptionTime
    }

    fun updateEventStatus(eventStatus: String) {
        this.eventStatus = eventStatus
    }

    fun updateName(name: String) {
        this.name = name
    }
}
