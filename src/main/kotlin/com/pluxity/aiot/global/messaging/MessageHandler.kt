package com.pluxity.aiot.global.messaging

import com.pluxity.aiot.global.messaging.dto.ChangeEventStatusPayload
import com.pluxity.aiot.global.messaging.dto.ConnectionErrorPayload
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageHandler(
    private val messageSender: StompMessageSender,
) {
    @MessageMapping("/test/connection-error")
    fun testConnectionError(
        @Payload payload: ConnectionErrorPayload,
    ) {
        messageSender.sendConnectionError(payload)
    }

    @MessageMapping("/test/sensor-alarm")
    fun testSensorAlarm(
        @Payload payload: SensorAlarmPayload,
    ) {
        messageSender.sendSensorAlarm(payload)
    }

    @MessageMapping("/test/change-event-status")
    fun testChangeEventStatus(
        @Payload payload: ChangeEventStatusPayload,
    ) {
        messageSender.changeEventStatus(payload)
    }
}
