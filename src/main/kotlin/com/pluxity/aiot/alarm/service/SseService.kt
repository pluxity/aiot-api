package com.pluxity.aiot.alarm.service

import com.pluxity.aiot.alarm.dto.AlarmEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.apache.catalina.connector.ClientAbortException
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class SseService {
    private val queueCapacity = 10000

    private val emitterTimeout: Long = 1800000

    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val connectionTimes = ConcurrentHashMap<String, Instant>()
    private lateinit var eventQueue: BlockingQueue<AlarmEvent>
    private lateinit var eventProcessorExecutor: ExecutorService

    private val activeConnections = AtomicInteger(0)
    private val totalEventsProcessed = AtomicLong(0)
    private val totalEventsDropped = AtomicLong(0)
    private val totalErrors = AtomicLong(0)

    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun initialize() {
        eventQueue = LinkedBlockingQueue(queueCapacity)

        eventProcessorExecutor =
            Executors.newSingleThreadExecutor { r: Runnable ->
                val thread = Thread(r, "sse-event-processor")
                thread.setDaemon(true)
                thread.setUncaughtExceptionHandler { _: Thread, e: Throwable ->
                    log.error(e) { "Uncaught exception in event processor thread: " }
                    totalErrors.incrementAndGet()
                    initialize()
                }
                thread
            }
        eventProcessorExecutor.submit { this.processEventQueue() }
        log.info { "SSE Event processor initialized with queue capacity: $queueCapacity" }
    }

    @PreDestroy
    fun shutdown() {
        shutdownExecutor()
        clearAllConnections()
        logFinalStats()
    }

    private fun shutdownExecutor() {
        if (!eventProcessorExecutor.isShutdown) {
            log.info { "Shutting down SSE Event processor" }
            eventQueue.clear()
            eventProcessorExecutor.shutdown()
            try {
                if (!eventProcessorExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn { "SSE Event processor did not terminate gracefully, forcing shutdown" }
                    eventProcessorExecutor.shutdownNow()
                    if (!eventProcessorExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                        error { "SSE Event processor forced shutdown failed" }
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn(e) { "Interrupted while shutting down SSE Event processor" }
                eventProcessorExecutor.shutdownNow()
            }
        }
    }

    private fun clearAllConnections() {
        if (!emitters.isEmpty()) {
            log.info { "Cleaning up $emitters.size remaining SSE connections" }
            emitters.values.forEach { emitter: SseEmitter -> this.completeEmitterSafely(emitter) }
            emitters.clear()
            connectionTimes.clear()
        }
    }

    private fun completeEmitterSafely(emitter: SseEmitter) {
        try {
            emitter.complete()
        } catch (e: Exception) {
            log.debug { "Error completing emitter: $e.message" }
        }
    }

    private fun logFinalStats() {
        log.info {
            "SSE Service shutdown stats - Processed: ${totalEventsProcessed.get()}, Dropped:${totalEventsDropped.get()}, Errors: ${totalErrors.get()}"
        }
    }

    @Scheduled(fixedRate = 300000)
    fun removeStaleConnections() {
        if (emitters.isEmpty()) {
            return
        }

        val now = Instant.now()
        val staleIds = findStaleConnections(now)

        val removedCount = staleIds.count { removeConnection(it) }

        if (removedCount > 0) {
            log.info { "Cleaned up $removedCount stale SSE connections" }
            activeConnections.set(emitters.size)
        }
    }

    private fun findStaleConnections(now: Instant): List<String> =
        connectionTimes.entries
            .filter { (clientId, connectTime) ->
                isStaleConnection(clientId, connectTime, now)
            }.map { it.key }
            .toList()

    private fun isStaleConnection(
        clientId: String,
        connectTime: Instant,
        now: Instant,
    ): Boolean =
        !emitters.containsKey(clientId) ||
            Duration.between(connectTime, now).toMillis() > emitterTimeout

    private fun removeConnection(clientId: String): Boolean =
        emitters.remove(clientId)?.let { emitter ->
            connectionTimes.remove(clientId)
            completeEmitterSafely(emitter)
            activeConnections.decrementAndGet()
            true
        } ?: false

    @Scheduled(fixedRate = 60000)
    fun logPeriodicStats() {
        log.info {
            "SSE Stats - Active connections: ${activeConnections.get()}, Queue size: ${eventQueue.size}, Processed: ${totalEventsProcessed.get()}, Dropped: ${totalEventsDropped.get()}, Errors: ${totalErrors.get()}"
        }
    }

    private fun processEventQueue() {
        log.info { "Started SSE event processing thread" }

        var backoffInterval: Long = 100

        try {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    backoffInterval = 100

                    val event = eventQueue.poll(500, TimeUnit.MILLISECONDS)
                    if (event != null) {
                        processEvent(event)
                    }
                } catch (_: InterruptedException) {
                    log.warn { "SSE event processor thread interrupted" }
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    totalErrors.incrementAndGet()
                    log.error(e) { "Error in SSE event processor" }
                    backoffInterval = performBackoff(backoffInterval)
                }
            }
        } finally {
            log.info { "SSE event processing thread terminated" }
        }
    }

    private fun processEvent(event: AlarmEvent) {
        if (emitters.isEmpty()) {
            log.debug { "No active SSE connections, skipping event: ${event.eventName}" }
            return
        }

        sendEventToAllClients(event)
        totalEventsProcessed.incrementAndGet()
    }

    private fun performBackoff(currentInterval: Long): Long {
        try {
            log.debug { "Backing off for $currentInterval ms after error" }
            eventQueue.poll(currentInterval, TimeUnit.MILLISECONDS)
            return minOf(currentInterval * 2, MAX_BACKOFF_INTERVAL)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            return currentInterval
        }
    }

    fun subscribe(clientId: String): SseEmitter {
        val validClientId = validateClientId(clientId)

        removeExistingEmitter(validClientId)

        val emitter: SseEmitter = createEmitter()
        registerCallbacks(emitter, validClientId)

        registerConnection(validClientId, emitter)

        return sendInitialMessage(emitter, validClientId)
    }

    private fun validateClientId(clientId: String?): String {
        if (clientId == null || clientId.trim { it <= ' ' }.isEmpty()) {
            val newId = System.currentTimeMillis().toString()
            log.info { "Empty client ID replaced with timestamp: $newId" }
            return newId
        }
        return clientId
    }

    private fun removeExistingEmitter(clientId: String?) {
        emitters.remove(clientId)?.let { oldEmitter ->
            completeEmitterSafely(oldEmitter)
            log.info { "Removed existing emitter for client: $clientId" }
            activeConnections.decrementAndGet()
        }
    }

    private fun createEmitter(): SseEmitter = SseEmitter(emitterTimeout)

    private fun registerCallbacks(
        emitter: SseEmitter,
        clientId: String,
    ) {
        emitter.onCompletion {
            log.info { "SSE connection completed for client: $clientId" }
            removeConnection(clientId)
        }

        emitter.onTimeout {
            log.info { "SSE connection timeout for client: $clientId" }
            removeConnection(clientId)
        }

        emitter.onError { e: Throwable ->
            log.warn { "SSE connection error for client: $clientId: ${e.message}" }
            removeConnection(clientId)
        }
    }

    private fun registerConnection(
        clientId: String,
        emitter: SseEmitter,
    ) {
        emitters[clientId] = emitter
        connectionTimes[clientId] = Instant.now()
        activeConnections.incrementAndGet()
    }

    private fun sendInitialMessage(
        emitter: SseEmitter,
        clientId: String,
    ): SseEmitter {
        try {
            emitter.send(
                SseEmitter
                    .event()
                    .name("connect")
                    .id(clientId)
                    .data("Connected successfully!"),
            )
            log.info { "Client connected successfully: $clientId" }
            return emitter
        } catch (e: Exception) {
            log.error(e) { "Error during initial connection: $clientId" }
            removeConnection(clientId)
            emitter.completeWithError(e)
            return emitter
        }
    }

    @Async("sseTaskExecutor")
    fun publish(event: AlarmEvent) {
        if (emitters.isEmpty()) {
            return
        }

        if (!eventQueue.offer(event)) {
            totalEventsDropped.incrementAndGet()
            log.warn { "Event queue is full, event discarded: $event.eventName" }
        } else if (log.isDebugEnabled()) {
            log.debug { "Event added to queue: $event.eventName" }
        }
    }

    private fun sendEventToAllClients(event: AlarmEvent) {
        val currentEmitters = ConcurrentHashMap(emitters)
        val deadEmitters = mutableListOf<String>()

        currentEmitters.forEach { (clientId: String, emitter: SseEmitter) ->
            processClientEmitter(
                clientId,
                emitter,
                event,
                deadEmitters,
            )
        }

        if (!deadEmitters.isEmpty()) {
            removeDeadEmitters(deadEmitters)
        }
    }

    private fun processClientEmitter(
        clientId: String,
        emitter: SseEmitter?,
        event: AlarmEvent,
        deadEmitters: MutableList<String>,
    ) {
        if (emitter == null || !emitters.containsKey(clientId)) {
            deadEmitters.add(clientId)
            return
        }

        try {
            emitter.send(
                SseEmitter
                    .event()
                    .name("alarm")
                    .data(event),
            )
        } catch (e: IOException) {
            deadEmitters.add(clientId)
            handleSendException(clientId, e)
        } catch (_: IllegalStateException) {
            deadEmitters.add(clientId)
            log.debug { "Emitter already completed for client: $clientId" }
        } catch (e: Exception) {
            deadEmitters.add(clientId)
            log.error(e) { "Unexpected error while sending event to client: $clientId" }
            totalErrors.incrementAndGet()
        }
    }

    private fun handleSendException(
        clientId: String,
        e: IOException,
    ) {
        if (e.cause is ClientAbortException) {
            log.debug { "Client disconnected: $clientId" }
        } else {
            log.error(e) { "Failed to send event to client: $clientId" }
            totalErrors.incrementAndGet()
        }
    }

    private fun removeDeadEmitters(deadEmitterIds: MutableList<String>?) {
        if (deadEmitterIds == null || deadEmitterIds.isEmpty()) {
            return
        }

        deadEmitterIds.forEach { clientId: String -> this.removeConnection(clientId) }
    }

    companion object {
        const val MAX_BACKOFF_INTERVAL: Long = 5000
    }
}
