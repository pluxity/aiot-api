package com.pluxity.aiot.domain.boundary

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.locationtech.jts.geom.Polygon

@Entity
class Boundary(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    val id: Long? = null,
    val name: String,
    @Column(columnDefinition = "POLYGON")
    var location: Polygon? = null,
) : BaseEntity()
