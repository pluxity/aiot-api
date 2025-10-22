package com.pluxity.aiot.global.messaging

import com.pluxity.aiot.global.messaging.dto.ConnectionErrorPayload
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageHandler(
    private val messageSender: StompMessageSender,
) {
    @MessageMapping("/test/connection-error")
    fun hello(
        @Payload payload: ConnectionErrorPayload,
    ) {
        messageSender.sendConnectionError(payload)
    }
}
