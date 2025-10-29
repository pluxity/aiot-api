package com.pluxity.aiot.feature.dto

data class FeatureSearchCondition(
    var siteId: Long? = null,
    var objectId: String? = null,
    var name: String? = null,
    var deviceId: String? = null,
    var isActive: Boolean? = null,
)
