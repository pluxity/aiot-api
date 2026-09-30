package com.pluxity.aiot.site

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class SiteLocator(
    private val siteRepository: SiteRepository,
) {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun <T> locate(
        items: List<T>,
        siteId: Long?,
        longitude: (T) -> Double?,
        latitude: (T) -> Double?,
    ): List<Pair<T, Site?>> {
        if (items.isEmpty()) return emptyList()
        val sites = if (siteId == null) siteRepository.findAll() else listOfNotNull(siteRepository.findByIdOrNull(siteId))
        val located = items.map { item -> item to findSite(sites, longitude(item), latitude(item)) }
        return if (siteId == null) located else located.filter { it.second != null }
    }

    // DB 쿼리의 ST_Contains와 맞춰 경계선 위의 점은 제외한다
    private fun findSite(
        sites: List<Site>,
        longitude: Double?,
        latitude: Double?,
    ): Site? {
        if (longitude == null || latitude == null) return null
        val point = geometryFactory.createPoint(Coordinate(longitude, latitude))
        return sites.firstOrNull { it.location.contains(point) }
    }
}
