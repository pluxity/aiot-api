package com.pluxity.aiot.alarm.service.processor

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.dto.SubscriptionConResponse
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.fixture.FeatureFixture
import com.pluxity.aiot.fixture.SiteFixture
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.profile.DeviceProfileType
import com.pluxity.aiot.system.device.type.DeviceType
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.ConditionType
import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import com.pluxity.aiot.system.event.condition.Operator

data class ConvertedConditionParams(
    val conditionType: ConditionType,
    val operator: Operator,
    val thresholdValue: Double?,
    val leftValue: Double?,
    val rightValue: Double?,
    val booleanValue: Boolean?,
)

fun convertLegacyConditionParams(
    isBoolean: Boolean,
    minValue: String?,
    maxValue: String?,
): ConvertedConditionParams =
    if (isBoolean) {
        val boolValue = minValue?.toBoolean() ?: true
        ConvertedConditionParams(
            conditionType = ConditionType.SINGLE,
            operator = Operator.GOE,
            thresholdValue = null,
            leftValue = null,
            rightValue = null,
            booleanValue = boolValue,
        )
    } else {
        val min = minValue?.toDoubleOrNull()
        val max = maxValue?.toDoubleOrNull()

        when {
            min != null && max != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.RANGE,
                    operator = Operator.BETWEEN,
                    thresholdValue = null,
                    leftValue = min,
                    rightValue = max,
                    booleanValue = null,
                )
            }
            min != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.GOE,
                    thresholdValue = min,
                    leftValue = null,
                    rightValue = null,
                    booleanValue = null,
                )
            }
            max != null -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.LOE,
                    thresholdValue = max,
                    leftValue = null,
                    rightValue = null,
                    booleanValue = null,
                )
            }
            else -> {
                ConvertedConditionParams(
                    conditionType = ConditionType.SINGLE,
                    operator = Operator.GOE,
                    thresholdValue = 0.0,
                    leftValue = null,
                    rightValue = null,
                    booleanValue = null,
                )
            }
        }
    }

fun mapDeviceEventLevelToConditionLevel(eventLevel: ConditionLevel): ConditionLevel =
    when (eventLevel) {
        ConditionLevel.NORMAL -> ConditionLevel.NORMAL
        ConditionLevel.WARNING -> ConditionLevel.WARNING
        ConditionLevel.CAUTION -> ConditionLevel.CAUTION
        ConditionLevel.DANGER -> ConditionLevel.DANGER
        ConditionLevel.DISCONNECTED -> ConditionLevel.DISCONNECTED
    }

abstract class ProcessorTestHelper(
    val deviceTypeRepository: DeviceTypeRepository,
    protected val deviceProfileRepository: DeviceProfileRepository,
    val siteRepository: SiteRepository,
    val featureRepository: FeatureRepository,
    protected val eventHistoryRepository: EventHistoryRepository,
    protected val actionHistoryService: ActionHistoryService,
    protected val eventConditionRepository: EventConditionRepository,
    protected val messageSenderMock: StompMessageSender,
    protected val writeApiMock: WriteApi,
) {
    private val profileCache = mutableMapOf<String, DeviceProfile>()

    fun getOrCreateProfile(
        fieldKey: String,
        description: String,
        fieldUnit: String,
        fieldType: DeviceProfile.FieldType,
    ): DeviceProfile =
        profileCache.getOrPut(fieldKey) {
            deviceProfileRepository.findAll().firstOrNull { it.fieldKey == fieldKey }
                ?: deviceProfileRepository.save(
                    DeviceProfile(
                        fieldKey = fieldKey,
                        description = description,
                        fieldUnit = fieldUnit,
                        fieldType = fieldType,
                    ),
                )
        }

    fun setupDeviceWithCondition(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        eventLevel: ConditionLevel,
        minValue: String? = null,
        maxValue: String? = null,
        isBoolean: Boolean = false,
    ): TestSetup {
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType =
                        DeviceType(
                            objectId = objectId,
                            description = "$objectId Description",
                            version = "1.0",
                        )
                    val deviceProfileType =
                        DeviceProfileType(
                            deviceProfile = profile,
                            deviceType = newDeviceType,
                        )
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        eventConditionRepository.deleteAllByObjectId(objectId)

        val (conditionType, operator, thresholdValue, leftValue, rightValue, booleanValue) =
            convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                fieldKey = profile.fieldKey,
                isActivate = true,
                level = level,
                conditionType = conditionType,
                operator = operator,
                thresholdValue = thresholdValue,
                leftValue = leftValue,
                rightValue = rightValue,
                booleanValue = booleanValue,
                notificationEnabled = true,
                order = 1,
            )
        eventConditionRepository.save(condition)

        val savedDeviceType = deviceType
        val site = siteRepository.save(SiteFixture.create(name = "Test Site $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId Sensor",
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    fun createSensorData(
        temperature: Double? = null,
        humidity: Double? = null,
        fireAlarm: Boolean? = null,
        angleX: Double? = null,
        angleY: Double? = null,
        timestamp: String = "20250115T103000",
    ): SubscriptionConResponse =
        SubscriptionConResponse(
            temperature = temperature,
            humidity = humidity,
            timestamp = timestamp,
            period = 60,
            fireAlarm = fireAlarm,
            angleX = angleX,
            angleY = angleY,
        )

    fun setupDeviceWithDisabledEvent(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        eventLevel: ConditionLevel,
        minValue: String?,
        maxValue: String?,
        isBoolean: Boolean = false,
    ): TestSetup {
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType = DeviceType(objectId = objectId, description = "$objectId Description", version = "1.0")
                    val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = newDeviceType)
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        eventConditionRepository.deleteAllByObjectId(objectId)

        val (conditionType, operator, thresholdValue, leftValue, rightValue, booleanValue) =
            convertLegacyConditionParams(isBoolean, minValue, maxValue)
        val level = mapDeviceEventLevelToConditionLevel(eventLevel)

        val condition =
            EventCondition(
                objectId = objectId,
                fieldKey = profile.fieldKey,
                isActivate = true,
                level = level,
                conditionType = conditionType,
                operator = operator,
                thresholdValue = thresholdValue,
                leftValue = leftValue,
                rightValue = rightValue,
                booleanValue = booleanValue,
                notificationEnabled = false,
                order = 1,
            )
        eventConditionRepository.save(condition)

        val savedDeviceType = deviceType
        val site = siteRepository.save(SiteFixture.create(name = "Test Site $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId Sensor",
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    fun setupDeviceWithMultipleConditions(
        objectId: String,
        deviceId: String,
        profile: DeviceProfile,
        conditions: List<ConditionSpec>,
    ): TestSetup {
        val deviceType =
            deviceTypeRepository.findAll().firstOrNull { it.objectId == objectId }
                ?: run {
                    val newDeviceType = DeviceType(objectId = objectId, description = "$objectId Description", version = "1.0")
                    val deviceProfileType = DeviceProfileType(deviceProfile = profile, deviceType = newDeviceType)
                    newDeviceType.deviceProfileTypes.add(deviceProfileType)
                    deviceTypeRepository.save(newDeviceType)
                }

        eventConditionRepository.deleteAllByObjectId(objectId)

        conditions.forEachIndexed { index, spec ->
            val (conditionType, operator, thresholdValue, leftValue, rightValue, booleanValue) =
                convertLegacyConditionParams(
                    spec.isBoolean,
                    spec.minValue,
                    spec.maxValue,
                )
            val level = mapDeviceEventLevelToConditionLevel(spec.eventLevel)

            val condition =
                EventCondition(
                    objectId = objectId,
                    fieldKey = profile.fieldKey,
                    isActivate = true,
                    level = level,
                    conditionType = conditionType,
                    operator = operator,
                    thresholdValue = thresholdValue,
                    leftValue = leftValue,
                    rightValue = rightValue,
                    booleanValue = booleanValue,
                    notificationEnabled = spec.notificationEnabled,
                    order = index + 1,
                )
            eventConditionRepository.save(condition)
        }

        val savedDeviceType = deviceType
        val site = siteRepository.save(SiteFixture.create(name = "Test Site $deviceId"))
        featureRepository.save(
            FeatureFixture.create(
                deviceId = deviceId,
                objectId = objectId,
                name = "$objectId Sensor",
                site = site,
            ),
        )

        return TestSetup(savedDeviceType, site.id!!)
    }

    data class ConditionSpec(
        val eventName: String,
        val eventLevel: ConditionLevel,
        val minValue: String?,
        val maxValue: String?,
        val needControl: Boolean = false,
        val isBoolean: Boolean = false,
        val notificationIntervalMinutes: Int = 5,
        val notificationEnabled: Boolean = true,
    )

    data class TestSetup(
        val deviceType: DeviceType,
        val siteId: Long,
    )
}
