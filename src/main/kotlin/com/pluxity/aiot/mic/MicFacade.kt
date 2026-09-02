package com.pluxity.aiot.mic

import com.pluxity.aiot.mic.dto.MicEventData
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicFacade(
    private val micClient: MicClient,
    private val micService: MicService,
    private val micEventService: MicEventService,
) {
    fun sync() {
        micService.sync(micClient.getMicList())
    }

    fun processEvent(eventData: MicEventData) {
        micEventService.saveEvent(eventData)
    }
}
