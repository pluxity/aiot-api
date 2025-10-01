package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.SseService
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.facility.FacilityRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.system.device.event.DeviceEvent
import com.pluxity.aiot.system.device.profile.DeviceProfile
import com.pluxity.aiot.system.device.profile.DeviceProfileRepository
import com.pluxity.aiot.system.device.type.DeviceTypeRepository
import com.pluxity.aiot.system.event.condition.EventCondition

/**
 * TemperatureHumidityProcessor 테스트를 위한 헬퍼 클래스
 */
class TemperatureHumidityProcessorTestHelper(
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
     * Temperature 조건으로 DeviceType 생성
     */
    fun setupTemperatureDevice(
        objectId: String,
        deviceId: String,
        eventName: String,
        eventLevel: DeviceEvent.DeviceLevel,
        minValue: Double,
        maxValue: Double,
        controlType: EventCondition.ControlType,
        guideMessage: String? = null,
    ): TestSetup =
        setupDeviceWithCondition(
            objectId = objectId,
            deviceId = deviceId,
            profile = temperatureProfile,
            eventName = eventName,
            eventLevel = eventLevel,
            minValue = minValue,
            maxValue = maxValue,
            operator = EventCondition.ConditionOperator.BETWEEN,
            controlType = controlType,
            guideMessage = guideMessage,
        )

    /**
     * TemperatureHumidityProcessor 인스턴스 생성
     */
    fun createProcessor(): TemperatureHumidityProcessor =
        TemperatureHumidityProcessor(
            sseServiceMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            writeApiMock,
        )
}
