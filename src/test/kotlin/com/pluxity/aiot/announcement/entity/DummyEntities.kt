package com.pluxity.aiot.announcement.entity

import com.pluxity.aiot.announcement.Announcement
import com.pluxity.aiot.base.entity.withAudit
import com.pluxity.aiot.base.entity.withId
import com.pluxity.aiot.site.entity.dummySite

fun dummyAnnouncement(
    id: Long = 1L,
    message: String,
    siteId: Long = 1L,
) = Announcement(
    site = dummySite(siteId),
    message = message,
).withAudit().withId(id)
