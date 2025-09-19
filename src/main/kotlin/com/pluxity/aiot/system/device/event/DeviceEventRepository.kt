package com.pluxity.aiot.system.device.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface DeviceEventRepository : JpaRepository<DeviceEvent, Long> {
    @Query("SELECT de FROM DeviceEvent de JOIN FETCH de.deviceType WHERE de.id = :id")
    fun findByIdWithDeviceType(id: Long): DeviceEvent?
}
