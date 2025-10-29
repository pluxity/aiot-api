package com.pluxity.aiot.feature.scheduler

import com.influxdb.annotations.Column
import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.data.AiotService
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

@Component
class FeatureScheduler(
    private val featureRepository: FeatureRepository,
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
    private val aiotService: AiotService,
) {
    @Profile("!local")
    @Scheduled(cron = "0 5 * * * *") // 매시간 5분에 실행
    @Transactional
    fun checkDisconnect() {
        val oneHourAgo = LocalDateTime.now().minusHours(1)
        val features = featureRepository.findAll()
        features.forEach { feature ->
            if (isDeviceDisconnected(feature, oneHourAgo)) {
                feature.updateEventStatus(ConditionLevel.DISCONNECTED.toString())
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

    @Profile("!local")
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    fun scheduledBatteryDataUpdate() {
        val features = featureRepository.findAll()
        runBlocking {
            supervisorScope {
                features
                    .map { feature ->
                        async {
                            // async로 병렬 처리
                            val batteryData = aiotService.fetchDeviceBatteryData(feature.deviceId)
                            feature.updateBatteryLevel(batteryData)
                            log.info { "deviceId: ${feature.deviceId}, Battery Level: $batteryData, update level: ${feature.batteryLevel}" }
                        }
                    }.awaitAll()
            }
        }
    }
}
