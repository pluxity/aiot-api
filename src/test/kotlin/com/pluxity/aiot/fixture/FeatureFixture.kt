package com.pluxity.aiot.fixture

import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.system.device.type.DeviceType
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

object FeatureFixture {
    private val gf = GeometryFactory(PrecisionModel(), 4326)

    fun create(
        id: Long? = null,
        deviceType: DeviceType? = null,
        deviceId: String = "DEVICE_001",
        objectId: String = "SENSOR_001",
        name: String? = "Test Device",
        longitude: Double? = 127.0,
        latitude: Double? = 37.0,
        geom: Point? = null,
        batteryLevel: Int? = 100,
        eventStatus: String? = "NORMAL",
        isActive: Boolean? = true,
        site: Site? = null,
    ): Feature {
        val point = geom ?: (longitude?.let { lon -> latitude?.let { lat -> gf.createPoint(Coordinate(lon, lat)) } })
        return Feature(
            id = id,
            deviceType = deviceType,
            deviceId = deviceId,
            objectId = objectId,
            name = name,
            longitude = longitude,
            latitude = latitude,
            geom = point,
            batteryLevel = batteryLevel,
            eventStatus = eventStatus,
            isActive = isActive,
            site = site,
        )
    }
}
