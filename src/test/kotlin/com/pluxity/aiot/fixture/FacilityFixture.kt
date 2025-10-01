package com.pluxity.aiot.fixture

import com.pluxity.aiot.facility.Facility
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel

object FacilityFixture {
    private val gf = GeometryFactory(PrecisionModel(), 4326)

    fun create(
        id: Long? = null,
        name: String = "Test Facility",
        description: String? = "Test facility description",
        location: Polygon = createDefaultPolygon(),
    ): Facility =
        Facility(
            id = id,
            name = name,
            description = description,
            location = location,
        )

    fun createDefaultPolygon(): Polygon {
        val coordinates =
            arrayOf(
                Coordinate(127.0, 37.0),
                Coordinate(127.1, 37.0),
                Coordinate(127.1, 37.1),
                Coordinate(127.0, 37.1),
                Coordinate(127.0, 37.0),
            )
        return gf.createPolygon(coordinates)
    }

    fun createCustomPolygon(
        minLon: Double,
        minLat: Double,
        maxLon: Double,
        maxLat: Double,
    ): Polygon {
        val coordinates =
            arrayOf(
                Coordinate(minLon, minLat),
                Coordinate(maxLon, minLat),
                Coordinate(maxLon, maxLat),
                Coordinate(minLon, maxLat),
                Coordinate(minLon, minLat),
            )
        return gf.createPolygon(coordinates)
    }

}