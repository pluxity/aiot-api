package com.pluxity.aiot.system.device.type

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceTypeRepository : JpaRepository<DeviceType, Long> {
    @EntityGraph(
        attributePaths = [
            "deviceProfileTypes",
            "deviceProfileTypes.deviceProfile",
        ],
    )
    override fun findAll(): List<DeviceType>

    @EntityGraph(
        attributePaths = [
            "deviceProfileTypes",
            "deviceProfileTypes.deviceProfile",
        ],
    )
    fun findByObjectId(objectId: String): DeviceType?

    @EntityGraph(
        attributePaths = [
            "deviceProfileTypes",
            "deviceProfileTypes.deviceProfile",
            "features",
        ],
    )
    @Query("SELECT dt FROM DeviceType dt WHERE dt.id = :id")
    fun findByIdWithAssociations(id: Long): DeviceType?
}
