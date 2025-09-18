package com.pluxity.aiot.abbreviation.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.pluxity.aiot.abbreviation.Abbreviation
import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.global.response.toBaseResponse

data class AbbreviationResponse(
    val id: Long,
    val type: String,
    val abbreviationKey: String,
    val fullName: String,
    val description: String,
    val isActive: Boolean,
    @field:JsonUnwrapped val baseResponse: BaseResponse,
)

fun Abbreviation.toAbbreviationResponse(): AbbreviationResponse =
    AbbreviationResponse(
        id = this.id!!,
        type = this.type,
        abbreviationKey = this.abbreviationKey,
        fullName = this.fullName,
        description = this.description!!,
        isActive = this.isActive,
        baseResponse = this.toBaseResponse(),
    )
