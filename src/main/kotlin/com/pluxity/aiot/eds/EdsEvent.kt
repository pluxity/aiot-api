package com.pluxity.aiot.eds

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(indexes = [Index(columnList = "eventId")])
class EdsEvent(
    @Column(unique = true, nullable = false)
    var index: Long,
    var eventId: Int,
    var profileName: String,
    var cameraId: String,
    @Enumerated(EnumType.STRING)
    var eventType: EdsEventType? = null,
    var eventStart: String,
    var eventEnd: String? = null,
    var frameTime: String? = null,
    @Enumerated(EnumType.STRING)
    var eventStatus: EdsEventStatus,
    var eventZoneId: Int? = null,
    var eventZoneName: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var detectedVehicleNumber: String? = null,
    var thumbnailFileId: Long? = null,
) : BaseEntity() {
    fun updateOnEnd(
        eventEnd: String?,
        frameTime: String?,
        eventStatus: EdsEventStatus,
    ) {
        this.eventEnd = eventEnd
        this.frameTime = frameTime
        this.eventStatus = eventStatus
    }
}
