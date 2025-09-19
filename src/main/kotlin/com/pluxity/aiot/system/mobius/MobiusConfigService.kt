package com.pluxity.aiot.system.mobius

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class MobiusConfigService(
    private val transactionService: MobiusTransactionService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    lateinit var currentUrl: String

    @PostConstruct
    fun init() {
        loadLatestUrl()
        log.info { "MobiusConfigService initialized with URL: $currentUrl" }
    }

    fun loadLatestUrl() {
        currentUrl = transactionService.loadLatestUrl().url
        log.info { "Mobius URL loaded: $currentUrl" }
    }

    fun createUrl(newUrl: String) {
        transactionService.createConfig(newUrl)
        currentUrl = newUrl
        eventPublisher.publishEvent(MobiusUrlUpdatedEvent(newUrl))
        log.info { "Mobius URL updated to: $newUrl" }
    }
}
