package com.pluxity.aiot.system.entity.deviceprofile

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceProfileRepository : JpaRepository<DeviceProfile, Long> {
    @EntityGraph(attributePaths = ["deviceProfileTypes", "deviceProfileTypes.deviceType"])
    override fun findAll(): List<DeviceProfile>

    fun existsByFieldKey(fieldKey: String): Boolean

    fun existsByFieldKeyAndIdNot(
        fieldKey: String,
        id: Long,
    ): Boolean
}
