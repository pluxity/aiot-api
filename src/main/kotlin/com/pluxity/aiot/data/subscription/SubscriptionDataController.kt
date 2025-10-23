package com.pluxity.aiot.data.subscription

import com.pluxity.aiot.data.subscription.dto.SubscriptionAlarm
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@Hidden
@RestController
class SubscriptionDataController(
    private val subscriptionDataService: SubscriptionDataService,
) {
    @PostMapping("/subscription")
    fun subscription(
        @RequestBody alarmRequest: SubscriptionAlarm,
    ) {
        log.info { "=== Subscription Notification Received ===" }
        log.info { "request: $alarmRequest" }

        subscriptionDataService.processData(alarmRequest)
    }
}
