package com.pluxity.aiot.feature.dto

data class FeatureUpdateRequest(
    val deviceTypeId: Long,
    val isActive: Boolean,
)
