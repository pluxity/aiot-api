package com.pluxity.aiot.speaker

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SpeakerPresetRepository : JpaRepository<SpeakerPreset, Long> {
    fun findAllByTitleContaining(
        title: String,
        pageable: Pageable,
    ): Page<SpeakerPreset>
}
