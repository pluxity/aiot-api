package com.pluxity.aiot.sensor

import com.influxdb.client.QueryApi
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import com.pluxity.aiot.alarm.dto.SubscriptionCinResponse
import com.pluxity.aiot.alarm.service.processor.SensorDataProcessor
import com.pluxity.aiot.alarm.type.SensorType
import com.pluxity.aiot.data.AiotService
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.ConnectionErrorPayload
import com.pluxity.aiot.global.properties.InfluxdbProperties
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.dto.LastSensorData
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

@Service
class SensorDataMigrationService(
    private val featureRepository: FeatureRepository,
    private val queryApi: QueryApi,
    private val influxdbProperties: InfluxdbProperties,
    private val aiotService: AiotService,
    processors: List<SensorDataProcessor>,
    private val messageSender: StompMessageSender,
) {
    private val processorMap: Map<String, SensorDataProcessor> =
        processors.associateBy { it.getObjectId() }

    /**
     * 모든 POI를 찾아서 각 device와 object에 대한 최신 데이터 이력을 가져와 저장
     */
    @Transactional
    fun migrateAllDataFromLastRecord() {
        log.info { "Starting migration of all sensor data from last records..." }

        val allFeatures = featureRepository.findAll()
        log.info { "Found ${allFeatures.size} features to process" }

        allFeatures.forEach { feature ->
            try {
                migrateDeviceData(feature.deviceId, feature.objectId, feature.site?.id!!)
            } catch (e: Exception) {
                log.error(e) { "Error migrating data for device: ${feature.deviceId}, object: ${feature.objectId}" }
            }
        }

        log.info { "Completed migration of all sensor data" }
    }

    /**
     * 특정 device와 object에 대한 데이터 이력을 가져와 저장
     */
    fun migrateDeviceData(
        deviceId: String,
        objectId: String,
        siteId: Long,
    ) {
        log.info { "Migrating data for device: $deviceId, object: $objectId" }

        // 해당 device와 object에 대한 가장 최근 record의 시간 가져오기
        val sensorType = SensorType.fromObjectId(objectId)
        val startTime = getLastRecordTime(deviceId, sensorType.measureName) ?: return
        val endTime = LocalDateTime.now()

        log.info { "Migrating data from $startTime to $endTime" }

        // Mobius 서버에서 데이터 가져오기
        val mobiusData = fetchMobiusData(deviceId, objectId, startTime, endTime)

        if (mobiusData == null || mobiusData.isEmpty()) {
            log.info { "No new data found in Mobius for device: $deviceId, object: $objectId" }
            return
        }

        log.info { "Found ${mobiusData.size} records in Mobius for device: $deviceId, object: $objectId" }

        // Sensor Data 저장
        for (sensorData in mobiusData) {
            val content = sensorData.con
            val timestamp = content.timestamp
            processorMap[objectId]?.insertSensorData(content, siteId, deviceId, timestamp)
        }
    }

    /**
     * 특정 device와 object에 대한 마지막 record의 시간 가져오기
     * 없으면 예외 발생
     */
    private fun getLastRecordTime(
        deviceId: String,
        measureName: String,
    ): LocalDateTime? {
        // 최신 기록 조회를 위한 쿼리
        val query =
            Flux
                .from(influxdbProperties.bucket)
                .range(-1L, ChronoUnit.YEARS)
                .filter(
                    Restrictions.and(
                        Restrictions.measurement().equal(measureName),
                        Restrictions.tag("deviceId").equal(deviceId),
                    ),
                ).sort(listOf("_time"), true)
                .limit(1)
                .keep(listOf("_time", "deviceId"))
                .toString()
        val data = queryApi.query(query, influxdbProperties.org, LastSensorData::class.java)

        if (data.isEmpty()) {
            // 기록이 없으면 예외 발생
            log.info { "센서 데이터가 없습니다. deviceId: $deviceId" }
            return null
        }

        return LocalDateTime.ofInstant(data[0].time, ZoneId.of("Asia/Seoul"))
    }

    /**
     * Mobius 서버에서 데이터 가져오기
     */
    private fun fetchMobiusData(
        deviceId: String,
        objectId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): List<SubscriptionCinResponse>? {
        // 날짜 형식 변환 - Mobius API는 yyyyMMdd'T'HHmmss 형식으로 요구함
        val startStr = DateTimeUtils.formatToTimestamp(startTime)
        val endStr = DateTimeUtils.formatToTimestamp(endTime)

        // Mobius API 호출
        return runBlocking {
            aiotService.findByDateRange(deviceId, "${objectId}_1.0_0", startStr, endStr)
        }
    }

    // 디바이스별 타이머 작업 저장
    private val deviceTimers = ConcurrentHashMap<String, ScheduledFuture<*>>()

    // 타이머 작업을 실행할 스케줄러
    private lateinit var scheduler: ScheduledExecutorService

    // 연속 실패 횟수 추적
    private val failureCountMap = ConcurrentHashMap<String, Int>()

    @PostConstruct
    private fun initScheduler() {
        scheduler = Executors.newScheduledThreadPool(4)
        log.info { "SensorDataMigrationService 타이머 스케줄러 초기화 완료" }
    }

    @PreDestroy
    private fun shutdownScheduler() {
        log.info { "SensorDataMigrationService 타이머 스케줄러 종료 중..." }
        // 먼저 모든 보류 중인 작업 취소
        for (entry in deviceTimers.entries) {
            entry.value.cancel(false)
            log.debug { "Timer canceled for device: ${entry.key}" }
        }
        deviceTimers.clear()

        // 그런 다음 스케줄러 종료
        scheduler.shutdown()
        try {
            // 대기 시간 증가 (10초에서 30초로)
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn { "SensorDataMigrationService 스케줄러가 정상적으로 종료되지 않았습니다. 강제 종료합니다." }
                scheduler.shutdownNow()
                // 추가 대기 시도
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    error { "SensorDataMigrationService 스케줄러 강제 종료 실패" }
                }
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
            log.warn(e) { "SensorDataMigrationService 스케줄러 종료 중 인터럽트 발생" }
        }
        log.info { "SensorDataMigrationService 타이머 스케줄러 종료 완료" }
    }

    /**
     * 새로운 센서 데이터가 수신되었을 때 호출
     * Reporting Period 기반으로 타이머를 설정하고 다음 데이터가 오지 않으면 복구 실행
     */
    fun registerSensorData(
        deviceId: String,
        objectId: String,
        siteId: Long,
        reportingPeriod: Int?,
    ) {
        val deviceKey = "$deviceId:$objectId"

        // 데이터가 정상적으로 수신되었으므로 실패 카운트 리셋
        failureCountMap[deviceKey] = 0

        // 기존 타이머 취소
        cancelExistingTimer(deviceKey)

        // 유효한 reporting period 확인
        val period = if (reportingPeriod != null && reportingPeriod > 0) reportingPeriod else 300

        // 여유 시간을 두고 타이머 설정 (1.5배 + 10초)
        val delaySeconds = (period * 1.5).toInt() + 10

        // 새 타이머 등록
        val future =
            scheduler.schedule({
                try {
                    log.info { "데이터 누락 감지 - deviceId: $deviceId, objectId: $objectId, Reporting Period: ${period}초" }

                    try {
                        migrateDeviceData(deviceId, objectId, siteId)
                        log.info { "데이터 복구 완료 - deviceId: $deviceId, objectId: $objectId" }

                        // 성공 시 실패 카운트 리셋
                        failureCountMap[deviceKey] = 0
                    } catch (_: Exception) {
                        // 실패 카운트 증가
                        val failureCount = failureCountMap.getOrDefault(deviceKey, 0) + 1
                        failureCountMap[deviceKey] = failureCount

                        log.warn {
                            "데이터 복구 실패 - deviceId: $deviceId, objectId: $objectId, " +
                                "연속 실패: $failureCount/${MAX_CONSECUTIVE_FAILURES}"
                        }

                        // 최대 실패 횟수 도달 시 SSE 알림 전송
                        if (failureCount >= MAX_CONSECUTIVE_FAILURES) {
                            sendConnectionFailureAlert(deviceId, objectId, failureCount, siteId)
                        }
                    }

                    // 타이머 재설정 (지속적 모니터링)
                    registerSensorData(deviceId, objectId, siteId, reportingPeriod)
                } catch (e: Exception) {
                    log.error(e) { "데이터 복구 처리 중 오류 - deviceId: $deviceId, objectId: $objectId" }
                } finally {
                    // 타이머 맵에서 제거
                    deviceTimers.remove(deviceKey)
                }
            }, delaySeconds.toLong(), TimeUnit.SECONDS)

        // 타이머 저장
        deviceTimers[deviceKey] = future

        log.debug { "데이터 타이머 설정 - deviceId: $deviceId, objectId: $objectId, 대기시간: ${delaySeconds}초" }
    }

    /**
     * 연속 실패 시 SSE 알림 전송
     */
    private fun sendConnectionFailureAlert(
        deviceId: String,
        objectId: String,
        failureCount: Int,
        siteId: Long,
    ) {
        val message = "디바이스 연결 오류 - $deviceId:$objectId 디바이스가 ${failureCount}회 연속 응답하지 않습니다."
        messageSender.sendConnectionError(
            ConnectionErrorPayload(
                siteId = siteId,
                deviceId = deviceId,
                objectId = objectId,
                message = message,
            ),
        )
        log.warn { "디바이스 연결 오류 알림 전송 - $message" }
    }

    /**
     * 기존 타이머 취소
     */
    private fun cancelExistingTimer(deviceKey: String) {
        val existingTimer = deviceTimers.get(deviceKey)
        if (existingTimer != null && !existingTimer.isDone) {
            existingTimer.cancel(false)
            log.debug { "기존 타이머 취소 - deviceKey: $deviceKey" }
        }
    }

    companion object {
        // 최대 허용 연속 실패 횟수
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }
}
