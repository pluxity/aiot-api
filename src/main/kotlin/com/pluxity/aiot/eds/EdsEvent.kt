package com.pluxity.aiot.eds

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity
class EdsEvent(
    @Column(unique = true, nullable = false)
    var index: Long,
    var eventId: Int,
    var profileName: String,
    var cameraId: String,
    var eventType: Int,
    var eventStart: String,
    var eventEnd: String? = null,
    var frameTime: String? = null,
    var status: Int,
    var eventZoneId: Int? = null,
    var eventZoneName: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var detectedVehicleNumber: String? = null,
    var thumbnailFileId: Long? = null,
) : BaseEntity() {

    fun updateOnEnd(eventEnd: String?, frameTime: String?, status: Int) {
        this.eventEnd = eventEnd
        this.frameTime = frameTime
        this.status = status
    }
}
