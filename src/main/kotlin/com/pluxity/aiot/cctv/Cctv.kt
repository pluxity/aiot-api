package com.pluxity.aiot.cctv

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

@Entity
class Cctv(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long? = null,
    var name: String = "",
    @Column(length = 1000)
    var url: String?,
    var longitude: Double? = null,
    var latitude: Double? = null,
    var height: Double? = null,
    @Column(columnDefinition = "geometry(Point, 4326)")
    var geom: Point? = null,
    @Column
    var mtxName: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    var site: Site? = null,
) : BaseEntity() {
    fun updateLocationInfo(
        longitude: Double,
        latitude: Double,
        site: Site?,
    ) {
        this.longitude = longitude
        this.latitude = latitude
        // Point 객체 생성 (SRID 4326 사용)
        val gf = GeometryFactory(PrecisionModel(), 4326)
        this.geom = gf.createPoint(Coordinate(longitude, latitude))
        this.site = site
    }

    fun updateMtxName(mtxName: String?) {
        this.mtxName = mtxName
    }

    fun updateCctv(
        name: String,
        url: String?,
        height: Double?,
    ) {
        this.name = name
        this.url = url
        this.height = height
    }

    fun updateLocationEmpty() {
        this.longitude = null
        this.latitude = null
        this.geom = null
        this.site = null
    }
}
