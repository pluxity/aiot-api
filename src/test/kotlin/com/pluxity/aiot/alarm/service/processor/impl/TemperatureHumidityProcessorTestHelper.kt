package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.data.subscription.processor.impl.TemperatureHumidityProcessor
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.event.condition.ConditionLevel
import com.pluxity.aiot.system.event.condition.EventConditionRepository

/**
 * TemperatureHumidityProcessor 테스트를 위한 헬퍼 클래스
 */
class TemperatureHumidityProcessorTestHelper(
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
    messageSenderMock: StompMessageSender,
    writeApiMock: WriteApi,
    eventConditionRepository: EventConditionRepository,
) : ProcessorTestHelper(
        siteRepository,
        featureRepository,
        eventHistoryRepository,
        actionHistoryService,
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
