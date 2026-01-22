package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.data.subscription.processor.ProcessorTestHelper
import com.pluxity.aiot.event.condition.ConditionLevel
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.sensor.type.DeviceProfileEnum
import com.pluxity.aiot.site.SiteRepository

/**
 * TemperatureHumidityProcessor 테스트를 위한 헬퍼 클래스
 */
class TemperatureHumidityProcessorTestHelper(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    eventHistoryRepository: EventHistoryRepository,
    messageSenderMock: StompMessageSender,
    writeApiMock: WriteApi,
    eventConditionRepository: EventConditionRepository,
) : ProcessorTestHelper(
        siteRepository,
        featureRepository,
        eventHistoryRepository,
        eventConditionRepository,
        messageSenderMock,
        writeApiMock,
    ) {
    /**
     * Temperature 조건으로 DeviceType 생성
     */
    fun setupTemperatureDevice(
        objectId: String,
        deviceId: String,
        eventLevel: ConditionLevel,
        minValue: Double,
        maxValue: Double,
        needControl: Boolean = false,
        guideMessage: String? = null,
        notificationIntervalMinutes: Int = 5,
    ): TestSetup =
        setupDeviceWithCondition(
            objectId = objectId,
            deviceId = deviceId,
            eventLevel = eventLevel,
            minValue = minValue.toString(),
            maxValue = maxValue.toString(),
            isBoolean = false,
            fieldKey = DeviceProfileEnum.TEMPERATURE.fieldKey,
        )

    /**
     * TemperatureHumidityProcessor 인스턴스 생성
     */
    fun createProcessor(): TemperatureHumidityProcessor =
        TemperatureHumidityProcessor(
            messageSenderMock,
            eventHistoryRepository,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
