package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.EventConditionRepository

/**
 * TemperatureHumidityProcessor 테스트를 위한 헬퍼 클래스
 */
class TemperatureHumidityProcessorTestHelper(
    deviceTypeRepository: DeviceTypeRepository,
    deviceProfileRepository: DeviceProfileRepository,
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
    messageSenderMock: StompMessageSender,
    writeApiMock: WriteApi,
    eventConditionRepository: EventConditionRepository,
) : ProcessorTestHelper(
        deviceTypeRepository,
        deviceProfileRepository,
        siteRepository,
        featureRepository,
        eventHistoryRepository,
        actionHistoryService,
        eventConditionRepository,
        messageSenderMock,
        writeApiMock,
    ) {
    /**
     * Temperature DeviceProfile (공유)
     */
    val temperatureProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "Temperature",
            description = "온도",
            fieldUnit = "℃",
            fieldType = DeviceProfile.FieldType.Float,
        )
    }

    /**
     * Humidity DeviceProfile (공유)
     */
    val humidityProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "Humidity",
            description = "습도",
            fieldUnit = "%",
            fieldType = DeviceProfile.FieldType.Float,
        )
    }

    /**
     * FireAlarm DeviceProfile (공유)
     */
    val fireAlarmProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "FireAlarm",
            description = "화재감지",
            fieldUnit = "boolean",
            fieldType = DeviceProfile.FieldType.Boolean,
        )
    }

    /**
     * Temperature 조건으로 DeviceType 생성
     */
    fun setupTemperatureDevice(
        objectId: String,
        deviceId: String,
        eventName: String,
        eventLevel: ConditionLevel,
        minValue: Double,
        maxValue: Double,
        needControl: Boolean = true,
        guideMessage: String? = null,
        notificationIntervalMinutes: Int = 0,
    ): TestSetup =
        setupDeviceWithCondition(
            objectId = objectId,
            deviceId = deviceId,
            profile = temperatureProfile,
            eventLevel = eventLevel,
            minValue = minValue.toString(),
            maxValue = maxValue.toString(),
            isBoolean = false,
        )

    /**
     * TemperatureHumidityProcessor 인스턴스 생성
     */
    fun createProcessor(): TemperatureHumidityProcessor =
        TemperatureHumidityProcessor(
            messageSenderMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
