package com.pluxity.aiot.facility.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.facility.Facility
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse

data class FacilityResponse(
    val id: Long?,
    val name: String,
    val description: String?,
    val location: String,
    @field:JsonUnwrapped val baseResponse: BaseResponse,
)

fun Facility.toFacilityResponse(): FacilityResponse =
    FacilityResponse(
        id = this.id,
        name = this.name,
        description = this.description,
        location = this.location.toText(),
        baseResponse = this.toBaseResponse(),
    )
