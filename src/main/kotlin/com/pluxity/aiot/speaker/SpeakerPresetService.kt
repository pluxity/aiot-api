package com.pluxity.aiot.speaker

import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.response.PageResponse
import com.pluxity.aiot.global.response.toPageResponse
import com.pluxity.aiot.speaker.dto.SpeakerPresetRequest
import com.pluxity.aiot.speaker.dto.SpeakerPresetResponse
import com.pluxity.aiot.speaker.dto.SpeakerPresetSearchRequest
import com.pluxity.aiot.speaker.dto.toResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SpeakerPresetService(
    private val speakerPresetRepository: SpeakerPresetRepository,
) {
    fun findAll(request: SpeakerPresetSearchRequest): PageResponse<SpeakerPresetResponse> {
        val pageable = PageRequest.of(request.page - 1, request.size, Sort.by(Sort.Direction.DESC, "id"))
        val page =
            request.title
                ?.takeIf { it.isNotBlank() }
                ?.let { speakerPresetRepository.findAllByTitleContaining(it, pageable) }
                ?: speakerPresetRepository.findAll(pageable)
        return page.toPageResponse { it.toResponse() }
    }

    fun findById(presetId: Long): SpeakerPresetResponse = findPreset(presetId).toResponse()

    @Transactional
    fun save(request: SpeakerPresetRequest): Long =
        speakerPresetRepository
            .save(SpeakerPreset(title = request.title, message = request.message, repeatCount = request.repeatCount))
            .requiredId

    @Transactional
    fun update(
        presetId: Long,
        request: SpeakerPresetRequest,
    ) {
        findPreset(presetId).update(request.title, request.message, request.repeatCount)
    }

    @Transactional
    fun delete(presetId: Long) {
        speakerPresetRepository.delete(findPreset(presetId))
    }

    private fun findPreset(presetId: Long): SpeakerPreset =
        speakerPresetRepository.findByIdOrNull(presetId) ?: throw CustomException(ErrorCode.NOT_FOUND_SPEAKER_PRESET, presetId)
}
