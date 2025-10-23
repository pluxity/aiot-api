package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.data.subscription.processor.impl.FireAlarmProcessor
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.event.condition.EventConditionRepository

/**
 * FireAlarmProcessor 테스트를 위한 헬퍼 클래스
 */
class FireAlarmProcessorTestHelper(
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
     * FireAlarmProcessor 인스턴스 생성
     */
    fun createProcessor(): FireAlarmProcessor =
        FireAlarmProcessor(
            messageSenderMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
