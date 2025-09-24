package com.pluxity.aiot.alarm.service.processor

import com.pluxity.aiot.action.ActionHistory
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.AlarmEvent
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.impl.DisplacementGaugeProcessor.Companion.ANGLE_X
import com.pluxity.aiot.alarm.service.processor.impl.DisplacementGaugeProcessor.Companion.ANGLE_Y
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.event.condition.EventCondition
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.math.abs

private val log = KotlinLogging.logger {}

interface SensorDataProcessor {
    companion object {
        private val lastNotificationMap: ConcurrentMap<String, LocalDateTime> = ConcurrentHashMap()
        private val featureCache: ConcurrentMap<String, Feature> = ConcurrentHashMap()
        private val featureCacheExpiryMap: ConcurrentMap<String, Long> = ConcurrentHashMap()
    }

    fun getObjectId(): String

    fun process(
        deviceId: String,
        deviceType: DeviceType,
        facilityId: Long,
        data: SubscriptionConResponse,
    )

    fun insertSensorData(
        content: SubscriptionConResponse,
        facilityId: Long,
        deviceId: String,
        timestamp: String,
    )

    fun processEvent(
        deviceId: String,
        deviceType: DeviceType,
        fieldKey: String,
        value: Double,
        profileType: DeviceProfileType,
        condition: EventCondition,
        feature: Feature?,
        parsedDate: LocalDateTime,
        sseService: SseService,
        eventHistoryRepository: EventHistoryRepository,
        actionHistoryService: ActionHistoryService,
        featureRepository: FeatureRepository,
    ) {
        var minValue = 0.0
        var maxValue = 0.0

        val conditionValue: String = condition.value
        if (!"true".equals(conditionValue, ignoreCase = true) && !"false".equals(conditionValue, ignoreCase = true)) {
            try {
                val range = conditionValue.split(",")
                minValue = range[0].toDouble()
                maxValue = if (range.size > 1) range[1].toDouble() else minValue
            } catch (e: NumberFormatException) {
                log.warn(e) { "조건값 파싱 오류: $conditionValue. 기본값 0.0 사용" }
            } catch (e: ArrayIndexOutOfBoundsException) {
                log.warn(e) { "조건값 파싱 오류: $conditionValue. 기본값 0.0 사용" }
            }
        }

        val eventName: String = condition.deviceEvent.name

        // POI의 이벤트 상태 업데이트
        updatePoiEventStatus(feature, condition.deviceEvent.deviceLevel.toString(), featureRepository)

        // 알림 간격 확인
        val notificationKey = "$deviceId:$eventName"
        val now = LocalDateTime.now()
        val lastNotificationTime = lastNotificationMap[notificationKey]

        // 조치 이력 처리 로직
        val isWithinNotificationInterval =
            lastNotificationTime != null &&
                condition.notificationIntervalMinutes > 0 &&
                now.isBefore(lastNotificationTime.plusMinutes(condition.notificationIntervalMinutes.toLong()))

        // 이벤트 이력 저장
        val eventHistory =
            eventHistoryRepository.save(
                EventHistory(
                    deviceId = deviceId,
                    objectId = deviceType.objectId,
                    sensorDescription = deviceType.description,
                    fieldKey = fieldKey,
                    value = value,
                    unit = profileType.deviceProfile?.fieldUnit,
                    eventName = eventName,
                    occurredAt = parsedDate,
                    minValue = minValue,
                    maxValue = maxValue,
                    guideMessage = condition.guideMessage,
                ),
            )

        val isAutoResponse = condition.isAutoResponseEnabled()

        // 조치 이력 처리 및 EventHistory 업데이트
        if (isAutoResponse && isWithinNotificationInterval) {
            // 6.1 조치가 자동이면서 무시 시간 안에 있으면 -> 무시(자동) 으로 조치 이력 저장, 무시 열은 true
            actionHistoryService.createAutomaticAction(
                deviceId,
                eventName,
                eventHistory,
                ActionHistory.ActionResult.IGNORED,
                true,
            )

            eventHistory.changeActionResult("AUTOMATIC_IGNORED")
            eventHistoryRepository.save(eventHistory)

            log.info { "Auto response - ignored due to notification interval: $notificationKey" }
            return // 이벤트 발행하지 않고 종료
        } else if (isAutoResponse && !isWithinNotificationInterval) {
            // 6.2 조치가 자동이면서 무시 시간 밖에 있으면 -> 자동 대응으로 저장, 무시 열은 false
            actionHistoryService.createAutomaticAction(
                deviceId,
                eventName,
                eventHistory,
                ActionHistory.ActionResult.COMPLETED,
                false,
            )

            eventHistory.changeActionResult("AUTOMATIC_COMPLETED")
            eventHistoryRepository.save(eventHistory)
        } else if (!isAutoResponse && isWithinNotificationInterval) {
            // 6.3 조치가 수동이면서 무시 시간 안에 있으면 -> 수동대응 (무시)으로 조치 이력 저장, 무시 열은 true
            actionHistoryService.createManualAction(
                deviceId,
                eventName,
                eventHistory,
                ActionHistory.ActionResult.IGNORED,
                true,
                parsedDate.toString(),
            )

            eventHistory.changeActionResult("MANUAL_IGNORED")
            eventHistoryRepository.save(eventHistory)

            log.info { "Manual response - ignored due to notification interval: $notificationKey" }
            return // 이벤트 발행하지 않고 종료
        } else {
            // 6.4 그 외의 경우에는 -> 수동 대응(조치전)으로 이벤트 히스토리 저장
            actionHistoryService.createManualAction(
                deviceId,
                eventName,
                eventHistory,
                ActionHistory.ActionResult.PENDING,
                false,
                parsedDate.toString(),
            )

            eventHistory.changeActionResult("MANUAL_PENDING")
            eventHistoryRepository.save(eventHistory)
        }
        // 현재 시간 업데이트 (알림 발생 시점 기록)
        lastNotificationMap[notificationKey] = now

        val message =
            "[$deviceId] ${profileType.deviceProfile?.description}: ${String.format("%.1f", value)} " +
                "${profileType.deviceProfile?.fieldUnit} - $eventName"

        // SSE로 이벤트 발행
        sseService.publish(
            AlarmEvent(
                sensorType = fieldKey,
                fieldKey = fieldKey,
                message = message,
                level = eventName,
                eventName = eventName,
                deviceId = deviceId,
                objectId = deviceType.objectId!!,
                sensorDescription = deviceType.description!!,
                value = value,
                unit = profileType.deviceProfile?.fieldUnit!!,
                minValue = minValue,
                maxValue = maxValue,
                notificationEnabled = condition.notificationEnabled,
                locationTrackingEnabled = condition.locationTrackingEnabled,
                soundEnabled = condition.soundEnabled,
                guideMessage = condition.guideMessage!!,
                actionResult = eventHistory.actionResult,
            ),
        )

        log.info { "Event triggered and saved: $message" }
    }

    fun processEventConditions(
        deviceId: String,
        deviceType: DeviceType,
        fieldKey: String,
        value: Double,
        timestamp: String,
        sseService: SseService,
        eventHistoryRepository: EventHistoryRepository,
        actionHistoryService: ActionHistoryService,
        featureRepository: FeatureRepository,
    ) {
        val parsedDate = DateTimeUtils.safeParseFromTimestamp(timestamp)

        // 해당 디바이스 ID로 POI 찾기 (캐시 사용)
        val feature: Feature? = getPoiFromCacheOrDb(deviceId, featureRepository)

        // 조건 충족을 위한 이벤트 컨테이너 준비
        val eventProcessingTasks: MutableList<CompletableFuture<Void>> = mutableListOf()

        // anyConditionMet 플래그를 위한 컨테이너
        val anyConditionMet = booleanArrayOf(false)
        deviceType.deviceProfileTypes
            .filter { profileType -> fieldKey == profileType.deviceProfile?.fieldKey }
            .forEach { profileType ->
                profileType.eventSettings
                    .filter { it.eventEnabled }
                    .filter {
                        !it.isPeriodic || it.months?.contains(LocalDate.now().monthValue) == true
                    }.forEach { eventSetting ->
                        eventSetting.conditions
                            .filter { condition ->
                                condition.deviceEvent.deviceLevel != DeviceEvent.DeviceLevel.NORMAL
                            }.filter { it.notificationEnabled }
                            .forEach { condition ->
                                if (isConditionMet(condition, value)) {
                                    anyConditionMet[0] = true

                                    // 비동기 이벤트 처리 작업 추가
                                    val eventTask =
                                        CompletableFuture.runAsync {
                                            processEvent(
                                                deviceId,
                                                deviceType,
                                                fieldKey,
                                                value,
                                                profileType,
                                                condition,
                                                feature,
                                                parsedDate,
                                                sseService,
                                                eventHistoryRepository,
                                                actionHistoryService,
                                                featureRepository,
                                            )
                                        }
                                    eventProcessingTasks.add(eventTask)
                                }
                            }
                    }
            }

        // 모든 이벤트 처리가 완료될 때까지 대기
        CompletableFuture.allOf(*eventProcessingTasks.toTypedArray<CompletableFuture<*>>()).join()

        // 조건을 만족하는 이벤트가 없으면 NORMAL 상태로 설정
        if (!anyConditionMet[0] && feature != null) {
            updatePoiEventStatus(feature, DeviceEvent.DeviceLevel.NORMAL.toString(), featureRepository)
        }
    }

    fun getPoiFromCacheOrDb(
        deviceId: String,
        featureRepository: FeatureRepository,
    ): Feature? {
        val currentTime = System.currentTimeMillis()

        // 캐시에 있고 만료되지 않은 경우 캐시된 값 반환
        if (featureCache.containsKey(deviceId)) {
            val expiryTime: Long? = featureCacheExpiryMap[deviceId]
            if (expiryTime != null && currentTime < expiryTime) {
                return featureCache[deviceId]
            }
        }

        // 캐시에 없거나 만료된 경우, DB에서 조회 후 캐시 업데이트
        val poiOpt = featureRepository.findByDeviceId(deviceId)
        featureCache[deviceId] = poiOpt
        featureCacheExpiryMap[deviceId] = currentTime + 864_000_000L
        return poiOpt
    }

    fun updatePoiEventStatus(
        feature: Feature?,
        eventStatus: String,
        featureRepository: FeatureRepository,
    ) {
        feature?.let {
            val dbFeature = featureRepository.findByIdOrNull(feature.id!!) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE, feature.id)
            dbFeature.updateEventStatus(eventStatus)
            featureRepository.save(dbFeature)
        }
    }

    fun isConditionMet(
        condition: EventCondition,
        value: Double,
    ): Boolean {
        val fieldKey: String? =
            condition.eventSetting
                ?.deviceProfileType
                ?.deviceProfile
                ?.fieldKey
        if (fieldKey != null && (fieldKey == ANGLE_X || fieldKey == ANGLE_Y)) {
            // 각도계 센서(X축, Y축)의 특별 처리: "오차값,중앙값" 형태를 "중앙값 ± 오차" 범위로 해석
            if (condition.operator === EventCondition.ConditionOperator.BETWEEN && condition.operator != null) {
                try {
                    val parts = condition.value.split(",")
                    if (parts.size == 2) {
                        val errorRange = parts[0].toDouble()
                        val centerValue = (if (fieldKey == ANGLE_X) 90 else 0).toDouble()

                        // 중앙값 ± 오차 범위 계산
                        val minRange = centerValue - errorRange
                        val maxRange = centerValue + errorRange

                        // 실제 값이 (중앙값-오차) ~ (중앙값+오차) 범위 내에 있는지 확인
                        return value <= minRange || value >= maxRange
                    }
                } catch (e: NumberFormatException) {
                    log.warn(e) { "각도계 조건값 파싱 오류: $condition.value" }
                }
            }
        }

        if (condition.operator === EventCondition.ConditionOperator.BETWEEN) {
            if (condition.minValue != null && condition.maxValue != null) {
                if (value >= condition.minValue!! && value <= condition.maxValue!!) {
                    val range = condition.value.split(",")
                    if (range.size == 2) {
                        val min = range[0].toDouble()
                        val max = range[1].toDouble()
                        return value in min..max
                    }
                }
            }
        } else if (condition.operator === EventCondition.ConditionOperator.GREATER_THAN) {
            return value > condition.value.toDouble()
        } else if (condition.operator === EventCondition.ConditionOperator.LESS_THAN) {
            return value < condition.value.toDouble()
        } else if (condition.operator === EventCondition.ConditionOperator.GREATER_THAN_OR_EQUAL) {
            return value >= condition.value.toDouble()
        } else if (condition.operator === EventCondition.ConditionOperator.LESS_THAN_OR_EQUAL) {
            return value <= condition.value.toDouble()
        } else if (condition.operator === EventCondition.ConditionOperator.EQUALS) {
            val conditionValue: String = condition.value
            if ("true".equals(conditionValue, ignoreCase = true) || "false".equals(conditionValue, ignoreCase = true)) {
                val booleanAsDouble = if ("true".equals(conditionValue, ignoreCase = true)) 1.0 else 0.0
                return abs(value - booleanAsDouble) < 0.001 // 부동소수점 비교를 위한 오차 범위 설정
            }
            return value.equals(conditionValue.toDouble())
        }
        return false
    }
}
