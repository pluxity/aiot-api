package com.pluxity.aiot.site.entity

import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.site.Site
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader

fun dummySite(
    id: Long = 1L,
    name: String = "siteName",
    description: String = "description",
): Site {
    val gf = GeometryFactory(PrecisionModel(), 4326)
    val wktReader = WKTReader(gf)
    val wkt = "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))"
    return Site(
        name,
        description = description,
        location =
            wktReader.read(wkt) as Polygon,
    ).apply { this.id = id }.withAudit()
}
