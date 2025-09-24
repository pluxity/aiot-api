package com.pluxity.aiot.feature

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import com.pluxity.aiot.feature.Feature
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FeatureRepository :
    JpaRepository<Feature, Long>,
    KotlinJdslJpqlExecutor {
    @EntityGraph(attributePaths = ["deviceType", "drawing", "floor"])
    @Query("SELECT p FROM Feature p WHERE p.objectId LIKE %:objectIdPart%")
    fun findByObjectIdContaining(
        @Param("objectIdPart") objectIdPart: String,
    ): List<Feature>

    fun deleteAllByDeviceIdIn(deviceIds: List<String>)

    fun findByIsActiveTrue(): List<Feature>

    fun findByDeviceId(deviceId: String): Feature?
}
