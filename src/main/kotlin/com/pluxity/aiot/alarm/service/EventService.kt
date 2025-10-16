package com.pluxity.aiot.alarm.service

import com.influxdb.annotations.Column
import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.alarm.dto.SubscriptionAlarm
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.sensor.SensorDataMigrationService
import com.pluxity.aiot.system.device.event.DeviceEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Service
class EventService(
    private val sensorDataMigrationService: SensorDataMigrationService,
    private val featureRepository: FeatureRepository,
    private val sensorDataHandler: SensorDataHandler,
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
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
                    featureRepository.findByDeviceId(deviceId)
                        ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE_BY_DEVICE_ID, deviceId)

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

    @Profile("!local")
    @Scheduled(cron = "0 5 * * * *") // 매시간 5분에 실행
    @Transactional
    fun checkDisconnect() {
        val oneHourAgo = LocalDateTime.now().minusHours(1)
        val features = featureRepository.findAll()
        features.forEach { feature ->
            if (isDeviceDisconnected(feature, oneHourAgo)) {
                feature.updateEventStatus(DeviceEvent.DeviceLevel.DISCONNECTED.toString())
            }
        }
    }

    private fun isDeviceDisconnected(
        feature: Feature,
        threshold: LocalDateTime,
    ): Boolean {
        return getLatestDate(feature)
            .firstOrNull()
            ?.time
            ?.let { instantTime ->
                val latestTime = instantTime.atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime()
                latestTime.isBefore(threshold)
            } ?: true // 데이터가 없으면 연결 끊어진 것으로 간주
    }

    private fun getLatestDate(feature: Feature): List<SensorLatestData?> {
        val sensorType = SensorType.fromObjectId(feature.objectId.take(5))
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(-1, ChronoUnit.DAYS)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(sensorType.measureName),
                        Restrictions.tag("deviceId").equal(feature.deviceId),
                    ),
                ).sort(listOf("_time"), true)
                .limit(1)
                .keep(listOf("_time"))
                .toString()

        return queryApi.query(query, influxdbProperties.org, SensorLatestData::class.java)
    }

    data class SensorLatestData(
        @Column(name = "_time") val time: Instant? = null,
    )
}
