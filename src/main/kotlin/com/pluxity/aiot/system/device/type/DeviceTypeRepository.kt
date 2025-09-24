package com.pluxity.aiot.system.device.type

import org.springframework.data.jpa.repository.JpaRepository

interface DeviceTypeRepository : JpaRepository<DeviceType, Long> {
    fun findFirstByObjectId(objectId: String): DeviceType?
}
