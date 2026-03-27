package com.pluxity.aiot.eds.dto

import com.pluxity.aiot.eds.EdsEvent
import com.pluxity.aiot.eds.EdsEventStatus
import com.pluxity.aiot.eds.EdsEventType
import com.pluxity.aiot.file.dto.FileResponse
import java.time.LocalDateTime

data class EdsEventResponse(
    val id: Long,
    val index: Long,
    val eventId: Int,
    val profileName: String,
    val cameraId: String,
    val eventType: EdsEventType?,
    val eventStart: String,
    val eventEnd: String?,
    val frameTime: String?,
    val eventStatus: EdsEventStatus,
    val eventZoneId: Int?,
    val eventZoneName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val detectedVehicleNumber: String?,
    val thumbnail: FileResponse,
    val createdAt: LocalDateTime,
)

fun EdsEvent.toResponse(thumbnail: FileResponse) =
    EdsEventResponse(
        id = requiredId,
        index = index,
        eventId = eventId,
        profileName = profileName,
        cameraId = cameraId,
        eventType = eventType,
        eventStart = eventStart,
        eventEnd = eventEnd,
        frameTime = frameTime,
        eventStatus = eventStatus,
        eventZoneId = eventZoneId,
        eventZoneName = eventZoneName,
        latitude = latitude,
        longitude = longitude,
        detectedVehicleNumber = detectedVehicleNumber,
        thumbnail = thumbnail,
        createdAt = createdAt,
    )
