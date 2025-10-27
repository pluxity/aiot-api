package com.pluxity.aiot.feature

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
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
    @Column(nullable = false, unique = true)
    var deviceId: String,
    @Column(nullable = false)
    var objectId: String,
    var name: String? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    var height: Double? = null,
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
    var subscriptionTime: LocalDateTime? = null,
) : BaseEntity() {
    fun updateActive(isActive: Boolean) {
        this.isActive = isActive
    }

    fun updateHeight(height: Double?) {
        this.height = height
    }

    fun updateInfo(
        name: String,
        objectId: String,
    ) {
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
        updateBatteryLevel(batteryLevel)
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

    fun updateBatteryLevel(batteryLevel: Int?) {
        this.batteryLevel = batteryLevel
    }
}
