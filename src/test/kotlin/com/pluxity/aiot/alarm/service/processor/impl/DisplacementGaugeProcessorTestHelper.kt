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
import com.pluxity.aiot.system.event.condition.EventConditionRepository

/**
 * DisplacementGaugeProcessor 테스트를 위한 헬퍼 클래스
 */
class DisplacementGaugeProcessorTestHelper(
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
     * AngleX DeviceProfile (공유)
     */
    val angleXProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "Angle-X",
            description = "X축 각도",
            fieldUnit = "°",
            fieldType = DeviceProfile.FieldType.Float,
        )
    }

    /**
     * AngleY DeviceProfile (공유)
     */
    val angleYProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "Angle-Y",
            description = "Y축 각도",
            fieldUnit = "°",
            fieldType = DeviceProfile.FieldType.Float,
        )
    }

    /**
     * DisplacementGaugeProcessor 인스턴스 생성
     */
    fun createProcessor(): DisplacementGaugeProcessor =
        DisplacementGaugeProcessor(
            this@DisplacementGaugeProcessorTestHelper.messageSenderMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
