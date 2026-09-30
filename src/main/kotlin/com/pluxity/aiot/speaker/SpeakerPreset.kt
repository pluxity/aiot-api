package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "speaker_preset")
class SpeakerPreset(
    @Column(nullable = false, length = 50)
    var title: String,
    @Column(nullable = false, length = 1000)
    var message: String,
    @Column(nullable = false)
    var repeatCount: Int,
) : BaseEntity() {
    fun update(
        title: String,
        message: String,
        repeatCount: Int,
    ) {
        this.title = title
        this.message = message
        this.repeatCount = repeatCount
    }
}
