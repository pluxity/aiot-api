package com.pluxity.aiot.system.device.type

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTypeRepository : JpaRepository<DeviceType, Long> {
    @EntityGraph(
        attributePaths = [
            "deviceProfileTypes",
            "deviceProfileTypes.deviceProfile",
            "deviceProfileTypes.eventSettings.conditions",
            "deviceProfileTypes.eventSettings.months",
            "deviceEvents",
        ],
    )
    override fun findAll(): List<DeviceType>
}
