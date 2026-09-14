package com.pluxity.aiot.action

import com.pluxity.aiot.global.entity.BaseEntity
import com.pluxity.aiot.incident.Incident
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "action_history")
class ActionHistory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    val incident: Incident,
    var content: String,
) : BaseEntity() {
    @OneToMany(mappedBy = "actionHistory")
    var historyFiles: MutableList<ActionHistoryFile> = mutableListOf()

    fun updateContent(content: String) {
        this.content = content
    }
}
