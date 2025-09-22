package com.pluxity.aiot.feature.dto

data class FeatureSearchCondition(
    var facilityId: Long? = null,
    var deviceTypeId: Long? = null,
    var name: String? = null,
    var deviceId: String? = null,
    var isActive: Boolean? = null,
)
