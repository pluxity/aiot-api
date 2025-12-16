package com.pluxity.aiot.cctv.entity

import com.pluxity.aiot.cctv.Cctv

fun dummyCctv(
    id: Long = 1L,
    name: String = "cctvName",
    url: String = "url",
    lon: Double = 127.0,
    lat: Double = 37.0,
): Cctv =
    Cctv(
        name = name,
        url = url,
        longitude = lon,
        latitude = lat,
    ).apply { this.id = id }
