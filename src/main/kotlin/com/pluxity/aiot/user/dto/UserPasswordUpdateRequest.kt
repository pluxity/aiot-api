package com.pluxity.aiot.user.dto

data class UserPasswordUpdateRequest(
    val currentPassword: String,
    val newPassword: String,
)
