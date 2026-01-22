package com.pluxity.aiot.cctv.entity

import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.cctv.Cctv
import com.pluxity.aiot.site.Site

fun dummyCctv(
    id: Long = 1L,
    name: String = "cctvName",
    url: String = "url",
    lon: Double = 127.0,
    lat: Double = 37.0,
    mtxName: String? = null,
    site: Site? = null,
): Cctv =
    Cctv(
        name = name,
        url = url,
        longitude = lon,
        latitude = lat,
        mtxName = mtxName,
        site = site,
    ).withId(id)
