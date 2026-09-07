package com.pluxity.aiot.feature

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동기화의 읽기 단계. 엔티티가 아니라 식별자만 내보낸다.
 *
 * 엔티티를 넘기면 뒤 트랜잭션에서 detached라 변경이 flush되지 않는다.
 */
@Service
class FeatureQueryService(
    private val featureRepository: FeatureRepository,
) {
    @Transactional(readOnly = true)
    fun findAllDeviceIds(): List<String> = featureRepository.findAll().map { it.deviceId }
}
