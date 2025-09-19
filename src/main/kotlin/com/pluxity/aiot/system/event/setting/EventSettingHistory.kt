package com.pluxity.aiot.system.event.setting

import com.pluxity.aiot.system.event.condition.EventCondition
import com.pluxity.aiot.system.event.condition.EventCondition.ConditionOperator
import com.pluxity.aiot.system.event.condition.EventCondition.ControlType
import jakarta.persistence.CollectionTable
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.time.LocalDateTime

@Entity
class EventSettingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_setting_id")
    var eventSetting: EventSetting? = null,
    @ElementCollection
    @CollectionTable(
        name = "event_setting_history_condition",
        joinColumns = [JoinColumn(name = "event_setting_history_id")],
    )
    var conditionInfos: MutableList<ConditionInfo> = mutableListOf(),
    var updatedAt: LocalDateTime? = null,
) {
    @Embeddable
    data class ConditionInfo(
        val deviceEventId: Long? = null,
        val deviceEventName: String? = null,
        @Enumerated(EnumType.STRING)
        val operator: ConditionOperator? = null,
        val value: String? = null,
        val minValue: Double? = null,
        val maxValue: Double? = null,
        val notificationEnabled: Boolean = false,
        val locationTrackingEnabled: Boolean = false,
        val soundEnabled: Boolean = false,
        @Enumerated(EnumType.STRING)
        val controlType: ControlType? = null,
        val guideMessage: String? = null,
        val notificationIntervalMinutes: Int? = null,
    ) {
        companion object {
            fun from(condition: EventCondition): ConditionInfo =
                EventSettingHistory.ConditionInfo(
                    condition.deviceEvent!!.id,
                    condition.deviceEvent.name,
                    condition.operator,
                    condition.value,
                    condition.minValue,
                    condition.maxValue,
                    condition.notificationEnabled,
                    condition.locationTrackingEnabled,
                    condition.soundEnabled,
                    condition.controlType,
                    condition.guideMessage,
                    condition.notificationIntervalMinutes,
                )
        }
    }

    companion object {
        fun of(eventSetting: EventSetting): EventSettingHistory {
            val history = EventSettingHistory()
            history.eventSetting = eventSetting
            history.updatedAt = LocalDateTime.now()

            history.conditionInfos =
                eventSetting.conditions.map { condition -> ConditionInfo.from(condition) }.toMutableList()

            return history
        }
    }
}
