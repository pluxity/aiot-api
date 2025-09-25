package com.pluxity.aiot.feature

import com.pluxity.aiot.facility.Facility
import com.pluxity.aiot.feature.dto.FeatureResponse
import com.pluxity.aiot.feature.dto.FeatureSearchCondition
import com.pluxity.aiot.feature.dto.FeatureUpdateRequest
import com.pluxity.aiot.feature.dto.toFeatureResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Service
class FeatureService(
    private val featureRepository: FeatureRepository,
    private val deviceTypeService: DeviceTypeService,
) {
    @Transactional(readOnly = true)
    fun findAll(searchCondition: FeatureSearchCondition? = null): List<FeatureResponse> {
        val features =
            featureRepository
                .findAll {
                    select(entity(Feature::class))
                        .from(
                            entity(Feature::class),
                            leftFetchJoin(Feature::facility),
                            leftFetchJoin(Feature::deviceType),
                        ).where(
                            and(
                                searchCondition?.facilityId?.let { path(Facility::id).equal(it) },
                                searchCondition?.deviceId?.let { path(Feature::deviceId).equal(it) },
                                searchCondition?.name?.let { path(Feature::name).equal(it) },
                                searchCondition?.deviceTypeId?.let { path(DeviceType::id).equal(it) },
                                searchCondition?.isActive?.let { path(Feature::isActive).equal(it) },
                            ),
                        )
                }.filterNotNull()

        return features.map { it.toFeatureResponse() }
    }

    @Transactional
    fun updateFeature(
        id: Long,
        request: FeatureUpdateRequest,
    ) {
        val feature = findById(id)
        if (feature.deviceType?.id != request.deviceTypeId) {
            val deviceType = deviceTypeService.findById(request.deviceTypeId)
            feature.updateDeviceType(deviceType)
        }
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

    private fun findById(id: Long) = featureRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE, id)
}
