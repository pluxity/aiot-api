package com.pluxity.aiot.user.repository

interface UserCustomRepository {
    fun getUserIdsWithSiteAccess(
        resourceType: String,
        resourceId: String,
    ): List<String>
}
