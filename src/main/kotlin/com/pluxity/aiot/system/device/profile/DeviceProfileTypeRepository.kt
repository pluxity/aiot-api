package com.pluxity.aiot.system.device.profile

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceProfileTypeRepository : JpaRepository<DeviceProfileType, Long> {
    @Query("SELECT dpt FROM DeviceProfileType dpt WHERE dpt.deviceType.objectId = :objectId")
    fun findAllByObjectId(objectId: String): List<DeviceProfileType>
}
