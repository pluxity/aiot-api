package com.pluxity.aiot.site.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.file.dto.FileResponse
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse
import com.pluxity.aiot.site.Site

data class SiteResponse(
    val id: Long?,
    val name: String,
    val description: String?,
    val location: String,
    @field:JsonUnwrapped val baseResponse: BaseResponse,
    val thumbnail: FileResponse? = null,
)

fun Site.toSiteResponse(fileMap: Map<Long, FileResponse>? = null): SiteResponse =
    SiteResponse(
        id = this.id,
        name = this.name,
        description = this.description,
        location = this.location.toText(),
        baseResponse = this.toBaseResponse(),
        thumbnail = fileMap?.let { fileMap[this.thumbnailId] },
    )
