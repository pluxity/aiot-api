package com.pluxity.aiot.feature.dto

import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse

data class FeatureResponse(
    val id: Long,
    val deviceId: String,
    val objectId: String,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val batteryLevel: Int?,
    val eventStatus: String,
    val isActive: Boolean,
    val siteResponse: SiteResponse?,
)

fun Feature.toFeatureResponse() =
    FeatureResponse(
        id = this.id!!,
        deviceId = this.deviceId,
        objectId = this.objectId.take(5),
        name = this.name!!,
        longitude = this.longitude!!,
        latitude = this.latitude!!,
        batteryLevel = this.batteryLevel,
        eventStatus = this.eventStatus!!,
        isActive = this.isActive!!,
        siteResponse = this.site?.toSiteResponse(),
    )
