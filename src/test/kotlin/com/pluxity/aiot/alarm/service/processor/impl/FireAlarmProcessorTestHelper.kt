package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.system.event.condition.EventConditionRepository
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository

/**
 * FireAlarmProcessor 테스트를 위한 헬퍼 클래스
 */
class FireAlarmProcessorTestHelper(
    deviceTypeRepository: DeviceTypeRepository,
    deviceProfileRepository: DeviceProfileRepository,
    siteRepository: SiteRepository,
    featureRepository: FeatureRepository,
    eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
    sseServiceMock: SseService,
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
        sseServiceMock,
        writeApiMock,
    ) {
    /**
     * FireAlarm DeviceProfile (공유)
     */
    val fireAlarmProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "Fire Alarm",
            description = "화재감지",
            fieldUnit = "boolean",
            fieldType = DeviceProfile.FieldType.Boolean,
        )
    }

    /**
     * FireAlarmProcessor 인스턴스 생성
     */
    fun createProcessor(): FireAlarmProcessor =
        FireAlarmProcessor(
            sseServiceMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
