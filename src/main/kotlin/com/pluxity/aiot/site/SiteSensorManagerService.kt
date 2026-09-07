package com.pluxity.aiot.site

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.site.dto.SiteSensorManagerResponse
import com.pluxity.aiot.site.dto.toManagerResponse
import com.pluxity.aiot.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SiteSensorManagerService(
    private val siteSensorManagerRepository: SiteSensorManagerRepository,
    private val siteRepository: SiteRepository,
    private val userRepository: UserRepository,
) {
    /** 지정된 카테고리가 없어도 전체 카테고리를 돌려줘, 화면이 목록을 따로 만들지 않게 한다 */
    fun findBySite(siteId: Long): List<SiteSensorManagerResponse> {
        requireSiteExists(siteId)
        val bySensorType =
            siteSensorManagerRepository
                .findAllBySiteIdOrderBySensorTypeAscIdAsc(siteId)
                .groupBy { it.sensorType }

        return SensorType.entries.map { sensorType ->
            SiteSensorManagerResponse(
                sensorType = sensorType,
                sensorTypeDescription = sensorType.description,
                managers = bySensorType[sensorType].orEmpty().map { it.toManagerResponse() },
            )
        }
    }

    /** 전체 교체. 빈 목록이면 해당 카테고리의 담당자를 모두 해제한다 */
    @Transactional
    fun replace(
        siteId: Long,
        sensorType: SensorType,
        userIds: List<Long>,
    ) {
        val site = siteRepository.findByIdOrNull(siteId) ?: throw CustomException(ErrorCode.NOT_FOUND_SITE, siteId)
        val distinctIds = userIds.distinct()
        val users = userRepository.findAllById(distinctIds)
        if (users.size != distinctIds.size) {
            val missing = distinctIds - users.mapNotNull { it.id }.toSet()
            throw CustomException(ErrorCode.NOT_FOUND_USER, "ID ${missing.joinToString()}")
        }

        val current = siteSensorManagerRepository.findAllBySiteIdAndSensorType(siteId, sensorType)
        siteSensorManagerRepository.deleteAll(current)
        // 유니크 제약 위반을 피하려면 같은 트랜잭션의 삭제가 삽입보다 먼저 나가야 한다
        siteSensorManagerRepository.flush()
        siteSensorManagerRepository.saveAll(users.map { SiteSensorManager(site, sensorType, it) })
    }

    /** 이벤트 알림 대상. 엔티티가 트랜잭션 밖으로 나가지 않도록 번호만 돌려준다 */
    fun findManagerPhoneNumbers(
        siteId: Long,
        sensorType: SensorType,
    ): List<String> =
        siteSensorManagerRepository
            .findAllBySiteIdAndSensorType(siteId, sensorType)
            .mapNotNull { it.user.phoneNumber?.takeIf(String::isNotBlank) }

    private fun requireSiteExists(siteId: Long) {
        if (!siteRepository.existsById(siteId)) throw CustomException(ErrorCode.NOT_FOUND_SITE, siteId)
    }
}
