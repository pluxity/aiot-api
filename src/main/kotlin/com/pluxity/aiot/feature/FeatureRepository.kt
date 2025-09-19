package com.pluxity.aiot.feature

import com.pluxity.aiot.feature.entity.Feature
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FeatureRepository : JpaRepository<Feature, Long> {
    @EntityGraph(attributePaths = ["deviceType", "drawing", "floor"])
    @Query("SELECT p FROM Feature p WHERE p.objectId LIKE %:objectIdPart%")
    fun findByObjectIdContaining(
        @Param("objectIdPart") objectIdPart: String,
    ): List<Feature>
}
