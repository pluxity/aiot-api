package com.pluxity.aiot.site.dto

import com.pluxity.aiot.base.dummyBaseResponse

fun dummySiteResponse(
    id: Long = 1L,
    name: String = "Test Site",
    description: String = "Test Site Description",
    location: String = "POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))",
): SiteResponse =
    SiteResponse(
        id = id,
        name = name,
        description = description,
        location = location,
        baseResponse = dummyBaseResponse(),
    )
