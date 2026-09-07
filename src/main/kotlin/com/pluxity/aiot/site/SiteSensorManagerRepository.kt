package com.pluxity.aiot.site

import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.user.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface SiteSensorManagerRepository : JpaRepository<SiteSensorManager, Long> {
    @EntityGraph(attributePaths = ["user"])
    fun findAllBySiteIdOrderBySensorTypeAscIdAsc(siteId: Long): List<SiteSensorManager>

    @EntityGraph(attributePaths = ["user"])
    fun findAllBySiteIdAndSensorType(
        siteId: Long,
        sensorType: SensorType,
    ): List<SiteSensorManager>

    fun deleteAllBySiteId(siteId: Long)

    fun deleteAllByUser(user: User)
}
