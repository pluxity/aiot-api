package com.pluxity.aiot.feature.entity

import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.site.Site
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

fun dummyFeature(
    id: Long? = null,
    deviceId: String = "DEVICE_001",
    objectId: String = "SENSOR_001",
    name: String? = "Test Device",
    longitude: Double? = 127.0,
    latitude: Double? = 37.0,
    geom: Point? = null,
    batteryLevel: Int? = 100,
    eventStatus: String = "NORMAL",
    isActive: Boolean = true,
    site: Site? = null,
): Feature {
    val gf = GeometryFactory(PrecisionModel(), 4326)
    val point = geom ?: (longitude?.let { lon -> latitude?.let { lat -> gf.createPoint(Coordinate(lon, lat)) } })
    return Feature(
        deviceId = deviceId,
        objectId = objectId,
        name = name,
    ).apply {
        this.longitude = longitude
        this.latitude = latitude
        this.geom = point
        this.batteryLevel = batteryLevel
        this.eventStatus = eventStatus
        this.isActive = isActive
        this.site = site
    }.withId(id)
        .withAudit()
}
