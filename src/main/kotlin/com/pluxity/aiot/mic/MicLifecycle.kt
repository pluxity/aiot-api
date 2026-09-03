package com.pluxity.aiot.mic

import com.pluxity.aiot.global.lifecycle.RetryingLifecycle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("mic.enabled", havingValue = "true")
class MicLifecycle(
    private val micClient: MicClient,
    private val micTokenScheduler: MicTokenScheduler,
    private val micFacade: MicFacade,
    private val micWebSocketClient: MicWebSocketClient,
) : RetryingLifecycle("AI 마이크") {
    override fun initialize() {
        micClient.login()
        micTokenScheduler.start()
        micFacade.sync()
        micWebSocketClient.connect()
    }

    override fun shutdown() {
        micWebSocketClient.disconnect()
        micTokenScheduler.stop()
    }
}
