package com.pluxity.aiot.cctv.entity

import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.eds.EdsCameraStatus
import com.pluxity.aiot.eds.EdsCameraType
import com.pluxity.aiot.site.Site

fun dummyCctv(
    id: Long = 1L,
    name: String = "cctvName",
    edsCameraId: String = "CAM-000001",
    lon: Double = 127.0,
    lat: Double = 37.0,
    ptzControl: Int? = 0,
    cameraType: EdsCameraType? = EdsCameraType.IP,
    cameraStatus: EdsCameraStatus? = EdsCameraStatus.NORMAL,
    site: Site? = null,
): Cctv =
    Cctv(
        name = name,
        edsCameraId = edsCameraId,
        longitude = lon,
        latitude = lat,
        ptzControl = ptzControl,
        cameraType = cameraType,
        cameraStatus = cameraStatus,
        site = site,
    ).withId(id)
