package com.pluxity.aiot.event.notification

import com.pluxity.aiot.site.SiteSensorManagerService
import com.pluxity.aiot.sms.SmsFacade
import com.pluxity.aiot.sms.SmsValidator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

/**
 * 발행 지점에 트랜잭션이 없어 `@TransactionalEventListener`는 이벤트를 흘린다.
 * `@Async`는 센서 처리 스레드를 붙잡지 않게 하고 [SmsFacade.send]의 "트랜잭션 밖" 조건도 만든다.
 * 수식어를 붙이면 AsyncConfig의 taskExecutor 빈으로 가지 않는다.
 */
@Component
class SensorEventSmsListener(
    private val siteSensorManagerService: SiteSensorManagerService,
    private val smsFacade: SmsFacade,
) {
    @Async
    @EventListener
    fun onSensorEvent(event: SensorEventNotified) {
        val targets = siteSensorManagerService.findManagerPhoneNumbers(event.siteId, event.sensorType)
        if (targets.isEmpty()) {
            log.debug { "담당자가 지정되지 않아 문자를 보내지 않습니다 (siteId=${event.siteId}, ${event.sensorType})" }
            return
        }

        try {
            smsFacade.send(title(event), message(event), targets)
        } catch (e: Exception) {
            // 문자 실패가 이벤트 처리 자체를 실패시키면 안 된다
            log.error(e) { "이벤트 문자 발송 실패 (eventId=${event.eventId})" }
        }
    }

    private fun title(event: SensorEventNotified): String =
        "[${event.siteName}] ${event.sensorType.description}".take(SmsValidator.MAX_TITLE_LENGTH)

    private fun message(event: SensorEventNotified): String =
        buildString {
            appendLine("[${event.level.name}] ${event.fieldDescription} ${formatValue(event.value)}${event.unit}")
            appendLine("장비: ${event.deviceId}")
            appendLine("발생: ${event.occurredAt.format(OCCURRED_AT_FORMAT)}")
            event.guideMessage?.takeIf { it.isNotBlank() }?.let { append(it) }
        }.trim().take(SmsValidator.MAX_MESSAGE_LENGTH)

    private fun formatValue(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.1f".format(value)

    companion object {
        private val OCCURRED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
