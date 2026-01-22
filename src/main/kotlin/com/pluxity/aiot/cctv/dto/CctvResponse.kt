package com.pluxity.aiot.cctv.dto

import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse

data class CctvResponse(
    val id: Long,
    val name: String,
    val viewUrl: String? = null,
    val url: String? = null,
    val lon: Double?,
    val lat: Double?,
    val height: Double?,
    val site: SiteResponse? = null,
)

fun Cctv.toCctvResponse(viewUrl: String) =
    CctvResponse(
        id = this.requiredId,
        name = this.name,
        url = this.url,
        viewUrl = this.mtxName?.let { "$viewUrl/${this.mtxName}" },
        lon = this.longitude,
        lat = this.latitude,
        height = this.height,
        site = this.site?.toSiteResponse(),
    )
