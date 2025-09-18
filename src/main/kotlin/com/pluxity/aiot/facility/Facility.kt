package com.pluxity.aiot.facility

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.permission.ResourceType
import com.pluxity.aiot.user.entity.Permissible
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.locationtech.jts.geom.Polygon

@Entity
@Table(name = "facility")
class Facility(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "name", nullable = false, length = 50)
    var name: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(columnDefinition = "geometry(Polygon, 4326)")
    var location: Polygon,
) : BaseEntity(),
    Permissible {
    //    @OneToMany(mappedBy = "facility")
//    val features: MutableList<Feature> = mutableListOf()

    fun updateName(name: String) {
        require(name.isNotBlank()) { "시설명은 빈 값일 수 없습니다" }
        this.name = name
    }

    fun updateDescription(description: String?) {
        this.description = description
    }

    fun updateLocation(location: Polygon) {
        this.location = location
    }

    override val resourceId: String
        get() = id.toString()

    override val resourceType: ResourceType
        get() = ResourceType.FACILITY
}
