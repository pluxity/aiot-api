package com.pluxity.aiot.eds

import com.pluxity.aiot.global.lifecycle.RetryingLifecycle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("eds.enabled", havingValue = "true")
class EdsLifecycle(
    private val edsClient: EdsClient,
    private val edsKeepAliveScheduler: EdsKeepAliveScheduler,
    private val edsFacade: EdsFacade,
    private val edsWebSocketClient: EdsWebSocketClient,
) : RetryingLifecycle("EDS") {
    override fun initialize() {
        edsClient.login()
        edsKeepAliveScheduler.start()
        edsFacade.sync()
        edsWebSocketClient.connect()
    }

    override fun shutdown() {
        edsWebSocketClient.disconnect()
        edsKeepAliveScheduler.stop()
    }
}
