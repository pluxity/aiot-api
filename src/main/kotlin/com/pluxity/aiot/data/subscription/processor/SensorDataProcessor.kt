package com.pluxity.aiot.data.subscription.processor

import com.pluxity.aiot.data.subscription.dto.SubscriptionConResponse
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.ConditionType
import com.pluxity.aiot.event.condition.EventCondition
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.condition.Operator
import com.pluxity.aiot.event.entity.EventHistory
import com.pluxity.aiot.event.notification.SensorEventNotified
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.Feature
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import com.pluxity.aiot.global.utils.DateTimeUtils
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.sensor.type.SensorType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
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
        eventPublisher: ApplicationEventPublisher,
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
                        eventId = eventHistory.requiredId,
                        deviceId = deviceId,
                        objectId = sensorType.objectId,
                        occurredAt = parsedDate.toString(),
                        minValue = minValue,
                        maxValue = maxValue,
                        status = eventHistory.status.name,
                        eventName = eventName,
                        fieldKey = fieldKey,
                        guideMessage = condition.guideMessage,
                        longitude = requireNotNull(feature.longitude) { "Feature(${feature.id}) longitude is null (not ready)" },
                        latitude = requireNotNull(feature.latitude) { "Feature(${feature.id}) latitude is null (not ready)" },
                        updatedAt = eventHistory.updatedAt.toString(),
                        updatedBy = eventHistory.updatedBy,
                        value = value,
                        level = condition.level.name,
                        siteId = it.id,
                        siteName = it.name,
                        sensorDescription = sensorType.description,
                        profileDescription = DeviceProfileEnum.getDescriptionByFieldKey(fieldKey),
                    ),
                )

                // 비동기 리스너의 등록이 거부되면 여기서 터진다. 측정값 적재까지 막으면 안 된다
                runCatching {
                    eventPublisher.publishEvent(
                        SensorEventNotified(
                            eventId = eventHistory.requiredId,
                            siteId = it.requiredId,
                            siteName = it.name,
                            deviceId = deviceId,
                            sensorType = sensorType,
                            level = condition.level,
                            fieldDescription = fieldDescription,
                            value = value,
                            unit = fieldUnit,
                            guideMessage = condition.guideMessage,
                            occurredAt = parsedDate,
                        ),
                    )
                }.onFailure { e -> log.error(e) { "이벤트 알림 발행 실패 (eventId=${eventHistory.requiredId})" } }
            }
        }

        log.info { "Event triggered and saved: $message" }
    }

    /**
     * 한 페이로드에 담긴 계측 항목 전체를 하나의 판정 단위로 처리한다.
     *
     * 복합 센서는 한 번에 여러 항목을 보고하므로 항목별로 즉시 상태를 반영하면
     * 조건이 등록되지 않은 뒤쪽 항목이 앞쪽 항목의 경보를 NORMAL로 덮어쓴다.
     * 따라서 전체 항목의 매칭 결과를 모은 뒤, 가장 높은 우선순위의 조건 하나만 반영하고
     * 매칭이 하나도 없을 때만 NORMAL로 되돌린다.
     */
    fun processEventConditions(
        deviceId: String,
        sensorType: SensorType,
        values: List<Pair<String, IncomingValue>>,
        timestamp: String,
        messageSender: StompMessageSender,
        eventHistoryRepository: EventHistoryRepository,
        featureRepository: FeatureRepository,
        eventConditionRepository: EventConditionRepository,
        eventPublisher: ApplicationEventPublisher,
    ) {
        val parsedDate = DateTimeUtils.safeParseFromTimestamp(timestamp)

        // 해당 디바이스 ID로 Feature 찾기 (캐시 사용)
        val feature: Feature = getFeatureFromCacheOrDb(deviceId, featureRepository)

        // 조건 대상 항목이 하나도 오지 않았다면 판단 근거가 없으므로 상태를 그대로 둔다
        val evaluable =
            values.mapNotNull { (fieldKey, value) ->
                sensorType.deviceProfiles.find { it.fieldKey == fieldKey }?.let { Triple(fieldKey, value, it) }
            }
        if (evaluable.isEmpty()) return

        val matched =
            evaluable.mapNotNull { (fieldKey, value, deviceProfile) ->
                eventConditionRepository
                    .findAllByObjectIdAndFieldKey(sensorType.objectId, fieldKey)
                    .filter { it.level != ConditionLevel.NORMAL && it.isActivate }
                    .sortedByDescending { it.level.priority }
                    .firstOrNull { isConditionMet(it, value) }
                    ?.let { MatchedCondition(fieldKey, value, deviceProfile, it) }
            }

        val winner =
            matched.maxByOrNull { it.condition.level.priority }
                ?: run {
                    updateFeatureEventStatus(feature, ConditionLevel.NORMAL.toString(), featureRepository)
                    return
                }

        processEvent(
            deviceId = deviceId,
            sensorType = sensorType,
            fieldKey = winner.fieldKey,
            value = winner.value.toEventHistoryValue(),
            fieldUnit = winner.deviceProfile.unit,
            fieldDescription = winner.deviceProfile.description,
            condition = winner.condition,
            feature = feature,
            parsedDate = parsedDate,
            messageSender = messageSender,
            eventHistoryRepository = eventHistoryRepository,
            featureRepository = featureRepository,
            eventPublisher = eventPublisher,
        )
    }

    /** 페이로드 내 한 계측 항목이 충족한 조건 */
    data class MatchedCondition(
        val fieldKey: String,
        val value: IncomingValue,
        val deviceProfile: DeviceProfileEnum,
        val condition: EventCondition,
    )

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
            val dbFeature =
                featureRepository.findByIdOrNull(feature.requiredId)
                    ?: throw CustomException(ErrorCode.NOT_FOUND_FEATURE, feature.id)
            dbFeature.updateEventStatus(eventStatus)
            featureRepository.save(dbFeature)
        }
    }

    fun isConditionMet(
        condition: EventCondition,
        incomingValue: IncomingValue,
    ): Boolean {
        // Boolean 값 체크
        if (condition.booleanValue != null) {
            val value = (incomingValue as? IncomingValue.Bool)?.value ?: return false
            return value == condition.booleanValue
        }

        // Numeric 값 체크
        val value = (incomingValue as? IncomingValue.Numeric)?.value ?: return false

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
}
