package com.pluxity.aiot.action

import com.pluxity.aiot.alarm.entity.EventHistory
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "action_history")
data class ActionHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_history_id", nullable = false)
    val eventHistory: EventHistory,
    @Column(nullable = false)
    var deviceId: String,
    @Column(nullable = false)
    var eventName: String,
    @Column(length = 5000)
    var content: String? = null,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var actionType: ActionType,
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var actionResult: ActionResult,
    @Column(nullable = false)
    var ignored: Boolean = false,
    @Column(nullable = false)
    var actedAt: LocalDateTime = LocalDateTime.now(),
    @Column
    var actedBy: String? = null,
) {
    enum class ActionType {
        AUTOMATIC, // 자동
        MANUAL, // 수동
    }

    enum class ActionResult(
        val value: String,
    ) {
        IGNORED("IGNORED"), // 무시
        PENDING("PENDING"), // 조치 전
        COMPLETED("COMPLETED"), // 조치 완료
    }
}
