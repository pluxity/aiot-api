package com.pluxity.aiot.feature

import com.pluxity.aiot.feature.dto.FeatureResponse
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
import com.pluxity.aiot.feature.dto.toFeatureResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.Site
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class FeatureService(
    private val featureRepository: FeatureRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(searchCondition: FeatureSearchCondition? = null): List<FeatureResponse> {
        val features =
            featureRepository
                .findAll {
                    select(entity(Feature::class))
                        .from(
                            entity(Feature::class),
                            leftFetchJoin(Feature::site),
                        ).where(
                            and(
                                searchCondition?.siteId?.let { path(Site::id).equal(it) },
                                searchCondition?.deviceId?.takeIf { it.isNotBlank() }?.let { path(Feature::deviceId).equal(it) },
                                searchCondition?.name?.takeIf { it.isNotBlank() }?.let { path(Feature::name).equal(it) },
                                searchCondition?.objectId?.let { path(Feature::objectId).equal(it) },
                                searchCondition?.isActive?.let { path(Feature::isActive).equal(it) },
                            ),
                        ).orderBy(path(Feature::site).asc())
                }.filterNotNull()

        return features.map { it.toFeatureResponse() }
    }

    @Transactional
    fun updateFeature(
        id: Long,
        request: FeatureUpdateRequest,
    ) {
        val feature = findById(id)
        feature.updateActive(request.isActive)
    }

    @Transactional
    fun updateFeatureName(
        id: Long,
        name: String,
    ) {
        val feature = findById(id)
        feature.updateName(name)
        log.info { "Updated Feature name: $name (id: $id)" }
    }

    @Transactional(readOnly = true)
    fun findByDeviceIdResponse(deviceId: String): FeatureResponse {
        val feature =
            featureRepository.findByDeviceId(deviceId) ?: throw CustomException(
                ErrorCode.NOT_FOUND_DEVICE_BY_FEATURE,
                deviceId,
            )

        return feature.toFeatureResponse()
    }

    private fun findById(id: Long) = featureRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE, id)
}
