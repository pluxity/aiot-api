package com.pluxity.aiot.global.messaging

import com.pluxity.aiot.global.messaging.component.SessionManager
import com.pluxity.aiot.global.messaging.dto.ChangeEventStatusPayload
import com.pluxity.aiot.global.messaging.dto.ConnectionErrorPayload
import com.pluxity.aiot.global.messaging.dto.SensorAlarmPayload
import com.pluxity.aiot.permission.ResourceType
import com.pluxity.aiot.user.repository.UserRepository
import io.github.springwolf.bindings.stomp.annotations.StompAsyncOperationBinding
import io.github.springwolf.core.asyncapi.annotations.AsyncOperation
import io.github.springwolf.core.asyncapi.annotations.AsyncPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class StompMessageSender(
    private val messageTemplate: SimpMessagingTemplate,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
) {
    companion object {
        const val QUEUE_CONNECTION_ERROR: String = "/queue/connection-error"
        const val QUEUE_SENSOR_ALARM: String = "/queue/sensor-alarm"
        const val CHANGE_EVENT_STATUS_ALARM: String = "/queue/change-event-status"
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = QUEUE_CONNECTION_ERROR,
                payloadType = ConnectionErrorPayload::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendConnectionError(payload: ConnectionErrorPayload) {
        userRepository.getUserIdsWithSiteAccess(ResourceType.SITE.name, payload.siteId.toString()).forEach { userId ->
            sessionManager.findPrincipalByUserId(userId).forEach { principal ->
                messageTemplate.convertAndSendToUser(principal.name, QUEUE_CONNECTION_ERROR, payload)
            }
        }
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = QUEUE_SENSOR_ALARM,
                payloadType = SensorAlarmPayload::class,
            ),
    )
    @StompAsyncOperationBinding
    fun sendSensorAlarm(payload: SensorAlarmPayload) {
        userRepository.getUserIdsWithSiteAccess(ResourceType.SITE.name, payload.siteId.toString()).forEach { userId ->
            sessionManager.findPrincipalByUserId(userId).forEach { principal ->
                messageTemplate.convertAndSendToUser(principal.name, QUEUE_SENSOR_ALARM, payload)
            }
        }
    }

    @AsyncPublisher(
        operation =
            AsyncOperation(
                channelName = CHANGE_EVENT_STATUS_ALARM,
                payloadType = ChangeEventStatusPayload::class,
            ),
    )
    @StompAsyncOperationBinding
    fun changeEventStatus(payload: ChangeEventStatusPayload) {
        userRepository.getUserIdsWithSiteAccess(ResourceType.SITE.name, payload.siteId.toString()).forEach { userId ->
            sessionManager.findPrincipalByUserId(userId).forEach { principal ->
                messageTemplate.convertAndSendToUser(principal.name, CHANGE_EVENT_STATUS_ALARM, payload)
            }
        }
    }
}
