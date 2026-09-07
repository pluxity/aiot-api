package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.data.subscription.processor.ProcessorTestHelper
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository

/**
 * ForestFireProcessor 테스트를 위한 헬퍼 클래스
 */
class ForestFireProcessorTestHelper(
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
    fun createProcessor(): ForestFireProcessor =
        ForestFireProcessor(
            messageSenderMock,
            eventHistoryRepository,
            featureRepository,
            eventConditionRepository,
            eventPublisherMock,
            writeApiMock,
        )
}
