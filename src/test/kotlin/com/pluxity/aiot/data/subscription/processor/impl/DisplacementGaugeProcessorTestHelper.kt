package com.pluxity.aiot.data.subscription.processor.impl

import com.influxdb.client.WriteApi
import com.pluxity.aiot.data.subscription.processor.ProcessorTestHelper
import com.pluxity.aiot.event.condition.EventConditionRepository
import com.pluxity.aiot.event.repository.EventHistoryRepository
import com.pluxity.aiot.feature.FeatureRepository
import com.pluxity.aiot.global.messaging.StompMessageSender
import com.pluxity.aiot.site.SiteRepository

/**
 * DisplacementGaugeProcessor 테스트를 위한 헬퍼 클래스
 */
class DisplacementGaugeProcessorTestHelper(
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
     * DisplacementGaugeProcessor 인스턴스 생성
     */
    fun createProcessor(): DisplacementGaugeProcessor =
        DisplacementGaugeProcessor(
            messageSenderMock,
            eventHistoryRepository,
            featureRepository,
            eventConditionRepository,
            writeApiMock,
        )
}
