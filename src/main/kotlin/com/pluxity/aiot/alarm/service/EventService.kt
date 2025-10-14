package com.pluxity.aiot.alarm.service

import com.pluxity.aiot.alarm.dto.SubscriptionAlarm
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.sensor.SensorDataMigrationService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class EventService(
    private val sensorDataMigrationService: SensorDataMigrationService,
    private val featureRepository: FeatureRepository,
    private val sensorDataHandler: SensorDataHandler,
) {
    fun processData(request: SubscriptionAlarm) {
        // 파라미터 추출
        try {
            // m2m:sgn에서 sur 추출하여 deviceId와 objectId 파싱
            val sur = request.sgn.sur

            val surParts = sur.split("/")
            if (surParts.size >= 4) {
                val deviceId = surParts[2]
                val objectId: String = surParts[3].split("_")[0]
                val feature =
                    featureRepository.findByDeviceId(deviceId) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE_BY_DEVICE_ID, deviceId)

                // con 데이터에서 reportingPeriod 추출
                val reportingPeriod = request.sgn.nev.rep.cin.con.period

                // 데이터 일관성 서비스에 등록
                sensorDataMigrationService.registerSensorData(deviceId, objectId, feature.site?.id!!, reportingPeriod)
                log.debug { "센서 데이터 모니터링 등록 - deviceId: $deviceId, objectId: $objectId, reportingPeriod: ${reportingPeriod}초" }
            }
        } catch (e: Exception) {
            log.warn { "데이터 일관성 체크 설정 중 오류: ${e.message}" }
        }

        // 센서 데이터 처리
        sensorDataHandler.handleData(request.sgn)
    }
}
