package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.facility.FacilityRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository

/**
 * DisplacementGaugeProcessor 테스트를 위한 헬퍼 클래스
 */
class DisplacementGaugeProcessorTestHelper(
    deviceTypeRepository: DeviceTypeRepository,
    deviceProfileRepository: DeviceProfileRepository,
    facilityRepository: FacilityRepository,
    featureRepository: FeatureRepository,
    eventHistoryRepository: EventHistoryRepository,
    actionHistoryService: ActionHistoryService,
    sseServiceMock: SseService,
    writeApiMock: WriteApi,
) : ProcessorTestHelper(
        deviceTypeRepository,
        deviceProfileRepository,
        facilityRepository,
        featureRepository,
        eventHistoryRepository,
        actionHistoryService,
        sseServiceMock,
        writeApiMock,
    ) {
    /**
     * AngleX DeviceProfile (공유)
     */
    val angleXProfile: DeviceProfile by lazy {
        getOrCreateProfile(
            fieldKey = "AngleX",
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
            fieldKey = "AngleY",
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
            sseServiceMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            writeApiMock,
        )
}
