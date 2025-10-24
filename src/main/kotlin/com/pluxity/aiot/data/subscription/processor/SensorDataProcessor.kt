package com.pluxity.aiot.data.subscription.processor

import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.entity.EventHistory
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.SensorType
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.DataType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import com.pluxity.aiot.system.event.condition.Operator
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
        actionHistoryService: ActionHistoryService,
        featureRepository: FeatureRepository,
    ) {
        val minValue = condition.numericValue1 ?: 0.0
        val maxValue = condition.numericValue2 ?: 0.0

        val eventName = "${condition.level.name}_$fieldKey"

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
                    eventName = condition.level.name,
                    occurredAt = parsedDate,
                    minValue = minValue,
                    maxValue = maxValue,
                ),
            )

        val message =
            "[$deviceId] $fieldDescription: ${String.format("%.1f", value)} " +
                "$fieldUnit - $eventName"

        feature.site?.let {
            messageSender.sendSensorAlarm(
                SensorAlarmPayload(
                    siteId = it.id!!,
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
                    notificationEnabled = condition.notificationEnabled,
                    actionResult = eventHistory.actionResult.name,
                ),
            )
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
        actionHistoryService: ActionHistoryService,
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
                }.filter { it.notificationEnabled }
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
                                    actionHistoryService,
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
        return when (condition.dataType) {
            DataType.NUMERIC -> {
                val value =
                    when (incomingValue) {
                        is Number -> incomingValue.toDouble()
                        else -> return false
                    }
                val v1 =
                    condition.numericValue1 ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_NUMERIC_VALUE, condition.numericValue1)

                when (condition.operator) {
                    Operator.GREATER_THAN -> value > v1
                    Operator.GREATER_OR_EQUAL -> value >= v1
                    Operator.LESS_THAN -> value < v1
                    Operator.LESS_OR_EQUAL -> value <= v1
                    Operator.EQUAL -> value == v1
                    Operator.NOT_EQUAL -> value != v1
                    Operator.BETWEEN -> {
                        val v2 =
                            condition.numericValue2
                                ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_NUMERIC_VALUE, condition.numericValue2)
                        value in v1..v2
                    }
                }
            }
            DataType.BOOLEAN -> {
                val value = incomingValue as? Boolean ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_BOOLEAN_VALUE, incomingValue)
                val bv = condition.booleanValue ?: throw CustomException(ErrorCode.NOT_FOUND_INVALID_BOOLEAN_VALUE, condition.booleanValue)

                when (condition.operator) {
                    Operator.EQUAL -> value == bv
                    Operator.NOT_EQUAL -> value != bv
                    else -> throw CustomException(ErrorCode.NOT_SUPPORTED_OPERATOR, condition.operator)
                }
            }
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
