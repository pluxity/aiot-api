package com.pluxity.aiot.feature

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 엔티티를 넘기면 뒤 트랜잭션에서 detached라 변경이 flush되지 않는다. */
@Service
class FeatureQueryService(
    private val featureRepository: FeatureRepository,
) {
    @Transactional(readOnly = true)
    fun findAllDeviceIds(): List<String> = featureRepository.findAll().map { it.deviceId }
}
