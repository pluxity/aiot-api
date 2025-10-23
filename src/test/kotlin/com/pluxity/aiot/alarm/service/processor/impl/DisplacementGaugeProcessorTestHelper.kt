package com.pluxity.aiot.alarm.service.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.action.ActionHistoryService
import com.pluxity.aiot.alarm.repository.EventHistoryRepository
import com.pluxity.aiot.alarm.service.processor.ProcessorTestHelper
import com.pluxity.aiot.data.subscription.processor.impl.DisplacementGaugeProcessor
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository
import com.pluxity.aiot.system.event.condition.EventConditionRepository

/**
 * DisplacementGaugeProcessor 테스트를 위한 헬퍼 클래스
 */
class DisplacementGaugeProcessorTestHelper(
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
     * DisplacementGaugeProcessor 인스턴스 생성
     */
    fun createProcessor(): DisplacementGaugeProcessor =
        DisplacementGaugeProcessor(
            messageSenderMock,
            eventHistoryRepository,
            actionHistoryService,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
