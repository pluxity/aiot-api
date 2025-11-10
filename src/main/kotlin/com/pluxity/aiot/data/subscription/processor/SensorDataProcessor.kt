package com.pluxity.aiot.data.subscription.processor

import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private val log = KotlinLogging.logger {}

interface SensorDataProcessor {
    companion object {
        private val featureCache: ConcurrentMap<String, Feature> = ConcurrentHashMap()
        private val featureCacheExpiryMap: ConcurrentMap<String, Long> = ConcurrentHashMap()
    }

    fun getObjectId(): String

    fun process(
        deviceId: String,
        sensorType: SensorType,
        siteId: Long,
        data: SubscriptionConResponse,
    )

    fun insertSensorData(
        content: SubscriptionConResponse,
        siteId: Long,
        deviceId: String,
        timestamp: String,
    )

    fun processEvent(
        deviceId: String,
        sensorType: SensorType,
        fieldKey: String,
        value: Double,
        fieldUnit: String,
        fieldDescription: String,
        condition: EventCondition,
        feature: Feature,
        parsedDate: LocalDateTime,
        messageSender: StompMessageSender,
        eventHistoryRepository: EventHistoryRepository,
        featureRepository: FeatureRepository,
    ) {
        val minValue = condition.thresholdValue ?: condition.leftValue ?: 0.0
        val maxValue = condition.rightValue ?: 0.0

        val eventName = "${condition.level.name}_$fieldKey"

        if (feature.eventStatus == condition.level.toString()) {
            log.info { "이벤트 상태가 이전과 동일 deviceId: $deviceId, status: ${feature.eventStatus}" }
            return
        }

        // Feature의 이벤트 상태 업데이트
        updateFeatureEventStatus(feature, condition.level.toString(), featureRepository)

        // 이벤트 이력 저장
        val eventHistory =
            eventHistoryRepository.save(
                EventHistory(
                    deviceId = deviceId,
                    objectId = sensorType.objectId,
                    sensorDescription = sensorType.description,
                    fieldKey = fieldKey,
                    value = value,
                    unit = fieldUnit,
                    eventName = eventName,
                    occurredAt = parsedDate,
                    minValue = minValue,
                    maxValue = maxValue,
                    guideMessage = condition.guideMessage,
                    longitude = feature.longitude,
                    latitude = feature.latitude,
                    level = condition.level,
                ),
            )

        val message =
            "[$deviceId] $fieldDescription: ${String.format("%.1f", value)} " +
                "$fieldUnit - $eventName"

        if (condition.notificationEnabled) {
            feature.site?.let {
                messageSender.sendSensorAlarm(
                    SensorAlarmPayload(
                        siteId = it.id!!,
                        siteName = it.name,
                        sensorType = sensorType.description,
                        fieldKey = fieldKey,
                        message = message,
                        level = eventName,
                        eventName = eventName,
                        deviceId = deviceId,
                        objectId = sensorType.objectId,
                        sensorDescription = sensorType.description,
                        value = value,
                        unit = fieldUnit,
                        minValue = minValue,
                        maxValue = maxValue,
                        status = eventHistory.status.name,
                        lon = feature.longitude!!,
                        lat = feature.latitude!!,
                        guideMessage = condition.guideMessage,
                    ),
                )
            }
        }

        log.info { "Event triggered and saved: $message" }
    }

    fun processEventConditions(
        deviceId: String,
        sensorType: SensorType,
        fieldKey: String,
        value: Any,
        timestamp: String,
        messageSender: StompMessageSender,
        eventHistoryRepository: EventHistoryRepository,
        featureRepository: FeatureRepository,
        eventConditionRepository: EventConditionRepository,
    ) {
        val parsedDate = DateTimeUtils.safeParseFromTimestamp(timestamp)

        // 해당 디바이스 ID로 Feature 찾기 (캐시 사용)
        val feature: Feature = getFeatureFromCacheOrDb(deviceId, featureRepository)

        // 조건 충족을 위한 이벤트 컨테이너 준비
        val eventProcessingTasks: MutableList<CompletableFuture<Void>> = mutableListOf()

        // anyConditionMet 플래그를 위한 컨테이너
        val anyConditionMet = booleanArrayOf(false)
        val conditions = eventConditionRepository.findAllByObjectId(sensorType.objectId)

        // sensorType.deviceProfiles에서 해당 fieldKey의 프로필 찾기
        val deviceProfile = sensorType.deviceProfiles.find { it.fieldKey == fieldKey }

        if (deviceProfile != null) {
            conditions
                .filter { condition ->
                    condition.level != ConditionLevel.NORMAL
                }.filter { it.isActivate }
                .forEach { condition ->
                    if (isConditionMet(condition, value, fieldKey)) {
                        anyConditionMet[0] = true

                        // value를 Double로 변환 (EventHistory 저장용)
                        val doubleValue =
                            when (value) {
                                is Number -> value.toDouble()
                                is Boolean -> if (value) 1.0 else 0.0
                                else -> 0.0
                            }

                        // 비동기 이벤트 처리 작업 추가
                        val eventTask =
                            CompletableFuture.runAsync {
                                processEvent(
                                    deviceId,
                                    sensorType,
                                    fieldKey,
                                    doubleValue,
                                    deviceProfile.unit,
                                    deviceProfile.description,
                                    condition,
                                    feature,
                                    parsedDate,
                                    messageSender,
                                    eventHistoryRepository,
                                    featureRepository,
                                )
                            }
                        eventProcessingTasks.add(eventTask)
                    }
                }
        }

        // 모든 이벤트 처리가 완료될 때까지 대기
        CompletableFuture.allOf(*eventProcessingTasks.toTypedArray<CompletableFuture<*>>()).join()

        // 조건을 만족하는 이벤트가 없으면 NORMAL 상태로 설정
        if (!anyConditionMet[0]) {
            updateFeatureEventStatus(feature, ConditionLevel.NORMAL.toString(), featureRepository)
        }
    }

    fun getFeatureFromCacheOrDb(
        deviceId: String,
        featureRepository: FeatureRepository,
    ): Feature {
        val currentTime = System.currentTimeMillis()

        // 캐시에 있고 만료되지 않은 경우 캐시된 값 반환
        if (featureCache.containsKey(deviceId)) {
            val expiryTime: Long? = featureCacheExpiryMap[deviceId]
            if (expiryTime != null && currentTime < expiryTime) {
                return featureCache[deviceId] ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE_BY_DEVICE_ID, deviceId)
            }
        }

        // 캐시에 없거나 만료된 경우, DB에서 조회 후 캐시 업데이트
        val feature =
            featureRepository.findByDeviceId(deviceId) ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE_BY_DEVICE_ID, deviceId)
        featureCache[deviceId] = feature
        featureCacheExpiryMap[deviceId] = currentTime + 864_000_000L
        return feature
    }

    fun updateFeatureEventStatus(
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
        incomingValue: Any,
    ): Boolean {
        // Boolean 값 체크
        if (condition.booleanValue != null) {
            val value = incomingValue as? Boolean ?: return false
            return value == condition.booleanValue
        }

        // Numeric 값 체크
        val value =
            when (incomingValue) {
                is Number -> incomingValue.toDouble()
                else -> return false
            }

        return when (condition.conditionType) {
            ConditionType.SINGLE -> {
                val threshold =
                    condition.thresholdValue
                        ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_NUMERIC_VALUE, "thresholdValue is null")

                when (condition.operator) {
                    Operator.GE -> value >= threshold
                    Operator.LE -> value <= threshold
                    Operator.BETWEEN -> throw CustomException(ErrorCode.NOT_SUPPORTED_OPERATOR, "BETWEEN not allowed for SINGLE type")
                    else -> false
                }
            }

            ConditionType.RANGE -> {
                val leftValue =
                    condition.leftValue
                        ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_NUMERIC_VALUE, "leftValue is null")
                val rightValue =
                    condition.rightValue
                        ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_NUMERIC_VALUE, "rightValue is null")

                when (condition.operator) {
                    Operator.BETWEEN -> value in leftValue..rightValue
                    else -> throw CustomException(ErrorCode.NOT_SUPPORTED_OPERATOR, "${condition.operator} not allowed for RANGE type")
                }
            }
            else -> false
        }
    }

    /**
     * 조건 충족 여부를 확인하는 메서드 (fieldKey 포함)
     * - 일반적으로는 fieldKey를 무시하고 isConditionMet(condition, value)를 호출
     * - 특수한 센서(예: DisplacementGauge)는 이 메서드를 override하여 fieldKey 기반 처리 가능
     */
    fun isConditionMet(
        condition: EventCondition,
        value: Any,
        fieldKey: String,
    ): Boolean = isConditionMet(condition, value)
}
