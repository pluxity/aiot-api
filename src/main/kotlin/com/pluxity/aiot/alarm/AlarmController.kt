package com.pluxity.aiot.alarm

import com.pluxity.aiot.alarm.dto.SubscriptionAlarm
import com.pluxity.aiot.alarm.service.EventService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@Hidden
@RestController
class AlarmController(
    private val eventService: EventService,
) {
    @PostMapping("/subscription")
    fun subscription(
        @RequestBody alarmRequest: SubscriptionAlarm,
    ) {
        log.info { "=== Subscription Notification Received ===" }
        log.info { "request: $alarmRequest" }

        eventService.processData(alarmRequest)
    }
}
