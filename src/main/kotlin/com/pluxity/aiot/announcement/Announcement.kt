package com.pluxity.aiot.announcement

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.site.Site
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Announcement(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    val site: Site,
    @Column(length = 1000)
    var message: String,
) : BaseEntity()
