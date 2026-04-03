package com.pluxity.aiot.cctv

import com.pluxity.aiot.eds.EdsCameraStatus
import com.pluxity.aiot.eds.EdsCameraType
import com.pluxity.aiot.eds.dto.EdsCameraInfo
import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

@Entity
class Cctv(
    var name: String = "",
    @Column(unique = true, nullable = false)
    var edsCameraId: String,
    var cameraIp: String? = null,
    var cameraPort: Int? = null,
    var longitude: Double? = null,
    var latitude: Double? = null,
    var ptzControl: Int? = null,
    @Enumerated(EnumType.STRING)
    var cameraType: EdsCameraType? = null,
    @Enumerated(EnumType.STRING)
    var cameraStatus: EdsCameraStatus? = null,
    var cameraAddress: String? = null,
    var streamResolutionWidth: Int? = null,
    var streamResolutionHeight: Int? = null,
    var cameraRecordType: Int? = null,
    var cameraAnalysisConfigured: Int? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    var site: Site? = null,
) : BaseEntity() {
    @Column(columnDefinition = "geometry(Point, 4326)")
    var geom: Point? = null

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

    companion object {
        private val GEOMETRY_FACTORY = GeometryFactory(PrecisionModel(), 4326)
    }

    fun updateLocationEmpty() {
        this.longitude = null
        this.latitude = null
        this.geom = null
        this.site = null
    }

    fun updateFromEds(
        edsCamera: EdsCameraInfo,
        site: Site?,
    ): Boolean {
        val newType = EdsCameraType.fromCode(edsCamera.cameraType)
        val newStatus = EdsCameraStatus.fromCode(edsCamera.cameraStatus)
        val newResW = edsCamera.streamResolution?.getOrNull(0)
        val newResH = edsCamera.streamResolution?.getOrNull(1)

        val changed =
            name != edsCamera.cameraName ||
                cameraIp != edsCamera.cameraIp ||
                cameraPort != edsCamera.cameraPort ||
                ptzControl != edsCamera.ptzControl ||
                cameraType != newType ||
                cameraStatus != newStatus ||
                cameraAddress != edsCamera.cameraAddress ||
                streamResolutionWidth != newResW ||
                streamResolutionHeight != newResH ||
                cameraRecordType != edsCamera.cameraRecordType ||
                cameraAnalysisConfigured != edsCamera.cameraAnalysisConfigured ||
                longitude != edsCamera.longitude ||
                latitude != edsCamera.latitude ||
                this.site?.id != site?.id

        if (!changed) return false

        this.name = edsCamera.cameraName
        this.cameraIp = edsCamera.cameraIp
        this.cameraPort = edsCamera.cameraPort
        this.ptzControl = edsCamera.ptzControl
        this.cameraType = newType
        this.cameraStatus = newStatus
        this.cameraAddress = edsCamera.cameraAddress
        this.streamResolutionWidth = newResW
        this.streamResolutionHeight = newResH
        this.cameraRecordType = edsCamera.cameraRecordType
        this.cameraAnalysisConfigured = edsCamera.cameraAnalysisConfigured
        if (edsCamera.longitude != null && edsCamera.latitude != null) {
            updateLocationInfo(edsCamera.longitude, edsCamera.latitude, site)
        } else {
            updateLocationEmpty()
        }
        return true
    }
}
