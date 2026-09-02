package com.pluxity.aiot.mic

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.mic.dto.MicInfo
import com.pluxity.aiot.mic.dto.MicThreshold
import com.pluxity.aiot.site.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

@Entity
class Mic(
    @Column(unique = true, nullable = false)
    var vendorMicId: String,
    var name: String? = null,
    var host: String? = null,
    var edgeId: String? = null,
    @Enumerated(EnumType.STRING)
    var status: MicStatus? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    /** 이벤트 카테고리별 감지 기준값. 카테고리가 가변이라 JSON으로 보관한다 */
    @JdbcTypeCode(SqlTypes.JSON)
    var thresholds: Map<String, MicThreshold>? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    var site: Site? = null,
) : BaseEntity() {
    @Column(columnDefinition = "geometry(Point, 4326)")
    var geom: Point? = null

    /**
     * 벤더 장비 정보로 갱신한다.
     * @return 실제로 바뀐 값이 있으면 true
     */
    fun updateFromVendor(
        micInfo: MicInfo,
        site: Site?,
    ): Boolean {
        val newStatus = MicStatus.fromValue(micInfo.status)
        val newLongitude = micInfo.location?.longitude
        val newLatitude = micInfo.location?.latitude

        val changed =
            name != micInfo.name ||
                host != micInfo.host ||
                edgeId != micInfo.edgeId ||
                status != newStatus ||
                thresholds != micInfo.thresholds ||
                longitude != newLongitude ||
                latitude != newLatitude ||
                this.site?.id != site?.id

        name = micInfo.name
        host = micInfo.host
        edgeId = micInfo.edgeId
        status = newStatus
        thresholds = micInfo.thresholds

        if (newLongitude != null && newLatitude != null) {
            updateLocationInfo(newLongitude, newLatitude, site)
        } else {
            updateLocationEmpty()
        }

        return changed
    }

    fun updateLocationInfo(
        longitude: Double,
        latitude: Double,
        site: Site?,
    ) {
        this.longitude = longitude
        this.latitude = latitude
        this.geom = GEOMETRY_FACTORY.createPoint(Coordinate(longitude, latitude))
        this.site = site
    }

    fun updateLocationEmpty() {
        this.longitude = null
        this.latitude = null
        this.geom = null
        this.site = null
    }

    fun disconnect() {
        this.status = MicStatus.DISCONNECTED
    }

    companion object {
        private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)
    }
}
