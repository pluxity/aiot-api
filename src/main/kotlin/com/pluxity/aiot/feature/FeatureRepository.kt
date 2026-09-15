package com.pluxity.aiot.feature

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FeatureRepository :
    JpaRepository<Feature, Long>,
    FeatureCustomRepository {
    @EntityGraph(attributePaths = ["deviceType", "drawing", "floor"])
    @Query("SELECT p FROM Feature p WHERE p.objectId LIKE %:objectIdPart%")
    fun findByObjectIdContaining(
        @Param("objectIdPart") objectIdPart: String,
    ): List<Feature>

    fun deleteAllByDeviceIdIn(deviceIds: List<String>)

    fun findAllByDeviceIdIn(deviceIds: List<String>): List<Feature>

    fun findByIsActiveTrueAndSiteIsNotNull(): List<Feature>

    /** 알림 처리 경로가 트랜잭션 밖에서 site 이름까지 읽고 캐시에 오래 들고 있어 프록시로 두면 안 된다. */
    @EntityGraph(attributePaths = ["site"])
    fun findByDeviceId(deviceId: String): Feature?
}
