package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayResponse
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import com.pluxity.aiot.ldms.dto.ldmsDeviceStatus
import com.pluxity.aiot.site.Site
import com.pluxity.aiot.site.SiteLocator
import com.pluxity.aiot.site.dto.toSiteResponse
import org.springframework.stereotype.Service

@Service
class DisplayService(
    private val ldmsClient: LdmsClient?,
    private val siteLocator: SiteLocator,
) {
    fun findAll(siteId: Long?): List<DisplayResponse> =
        siteLocator
            .locate(ldmsClient?.getEbrdList().orEmpty(), siteId, longitude = { it.ebrdLng }, latitude = { it.ebrdLat })
            .map { (display, site) -> display.toResponse(site) }
}

private fun LdmsEbrdInfo.toResponse(site: Site?) =
    DisplayResponse(
        id = ebrdSeq,
        name = ebrdNm.orEmpty(),
        deviceId = ebrdId.orEmpty(),
        location = ebrdDesc,
        latitude = ebrdLat,
        longitude = ebrdLng,
        status = ldmsDeviceStatus(commStat),
        site = site?.toSiteResponse(),
    )
