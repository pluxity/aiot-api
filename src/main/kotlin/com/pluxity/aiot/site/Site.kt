package com.pluxity.aiot.site

import com.pluxity.aiot.announcement.Announcement
import com.pluxity.aiot.announcement.LlmMessage
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.permission.ResourceType
import com.pluxity.aiot.user.entity.Permissible
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.locationtech.jts.geom.Polygon

@Entity
@Table(name = "site")
class Site(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "name", nullable = false, length = 50)
    var name: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(columnDefinition = "geometry(Polygon, 4326)")
    var location: Polygon,
    var thumbnailId: Long? = null,
) : BaseEntity(),
    Permissible {
    val requiredId: Long
        get() = checkNotNull(id) { "Site is not persisted yet" }

    @OneToMany(mappedBy = "site")
    val features: MutableList<Feature> = mutableListOf()

    @OneToMany(mappedBy = "site", cascade = [CascadeType.REMOVE])
    val llmMessages: MutableList<LlmMessage> = mutableListOf()

    @OneToMany(mappedBy = "site", cascade = [CascadeType.REMOVE])
    val announcements: MutableList<Announcement> = mutableListOf()

    fun updateName(name: String) {
        require(name.isNotBlank()) { "현장명은 빈 값일 수 없습니다" }
        this.name = name
    }

    fun updateDescription(description: String?) {
        this.description = description
    }

    fun updateLocation(location: Polygon) {
        this.location = location
    }

    fun updateThumbnailId(thumbnailId: Long?) {
        this.thumbnailId = thumbnailId
    }

    override val resourceId: String
        get() = id.toString()

    override val resourceType: ResourceType
        get() = ResourceType.SITE
}
