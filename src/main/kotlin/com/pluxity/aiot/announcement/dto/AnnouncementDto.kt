package com.pluxity.aiot.announcement.dto

import com.pluxity.aiot.announcement.Announcement
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class BroadcastRequest(
    @field:Schema(
        description = "메시지",
        example = "안내방송입니다.",
    )
    @field:NotBlank(message = "메시지는 필수 입니다.")
    val message: String,
    @field:Schema(
        description = "현장 아이디",
        example = "1",
    )
    @field:NotEmpty(message = "현장아이디는 필수 입니다.")
    val siteIds: List<Long>,
)

data class SearchRequest(
    val page: Int = 1,
    val size: Int = 10,
    val from: String? = null,
    val to: String? = null,
    val userId: String? = null,
    val siteId: Long? = null,
)

data class AnnouncementResponse(
    val id: Long,
    val message: String,
    val userId: String,
    val site: SiteResponse,
    val createdAt: String,
)

fun Announcement.toAnnouncementResponse() =
    AnnouncementResponse(
        id = this.id!!,
        message = this.message,
        userId = this.createdBy!!,
        site = this.site.toSiteResponse(),
        createdAt = this.createdAt.toString(),
    )
