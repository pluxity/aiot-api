package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayResponse
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import com.pluxity.aiot.ldms.dto.ldmsDeviceStatus
import org.springframework.stereotype.Service

@Service
class DisplayService(
    private val ldmsClient: LdmsClient?,
) {
    fun findAll(siteId: Long?): List<DisplayResponse> {
        val displays = ldmsClient?.getEbrdList().orEmpty().map { it.toResponse() }
        return if (siteId == null) displays else displays.filter { it.site?.id == siteId }
    }
}

private fun LdmsEbrdInfo.toResponse() =
    DisplayResponse(
        id = ebrdSeq,
        name = ebrdNm.orEmpty(),
        deviceId = ebrdId.orEmpty(),
        location = ebrdDesc,
        latitude = ebrdLat,
        longitude = ebrdLng,
        status = ldmsDeviceStatus(commStat),
        site = null,
    )
