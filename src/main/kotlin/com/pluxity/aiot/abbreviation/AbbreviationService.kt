package com.pluxity.aiot.abbreviation

import com.pluxity.aiot.abbreviation.dto.AbbreviationRequest
import com.pluxity.aiot.abbreviation.dto.AbbreviationResponse
import com.pluxity.aiot.abbreviation.dto.toAbbreviationResponse
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AbbreviationService(
    private val abbreviationRepository: AbbreviationRepository,
//    private val aiotService: AiotService,
//    private val poiService: PoiService
) {
    @Transactional(readOnly = true)
    fun getAllAbbreviations(): List<AbbreviationResponse> = abbreviationRepository.findAll().map { it.toAbbreviationResponse() }

    @Transactional(readOnly = true)
    fun getAbbreviationById(id: Long): AbbreviationResponse = findById(id).toAbbreviationResponse()

    @Transactional
    fun createAbbreviation(request: AbbreviationRequest): Long {
        if (abbreviationRepository.existsByAbbreviationKey(request.abbreviationKey)) {
            throw CustomException(ErrorCode.DUPLICATE_ABBREVIATION, request.abbreviationKey)
        }

        val abbreviation =
            Abbreviation(
                type = request.type,
                abbreviationKey = request.abbreviationKey,
                fullName = request.fullName,
                description = request.description,
                isActive = request.isActive,
            )
        updatePoiNamesByAbbreviations()
        return abbreviationRepository.save(abbreviation).id!!
    }

    @Transactional
    fun updateAbbreviation(
        id: Long,
        request: AbbreviationRequest,
    ) {
        val abbreviation = findById(id)
        if (abbreviationRepository.existsByAbbreviationKeyAndIdNot(request.abbreviationKey, id)) {
            throw CustomException(ErrorCode.DUPLICATE_ABBREVIATION, request.abbreviationKey)
        }
        abbreviation.update(request)
        updatePoiNamesByAbbreviations()
    }

    @Transactional
    fun deleteAbbreviation(id: Long) {
        findById(id)
        abbreviationRepository.deleteById(id)
    }

    fun findById(id: Long): Abbreviation =
        abbreviationRepository.findByIdOrNull(id) ?: throw CustomException(ErrorCode.NOT_FOUND_ABBREVIATION, id)

    private fun updatePoiNamesByAbbreviations() {
        // TODO 작업필요
//        val abbreviationList = abbreviationRepository.findAll()
//        val poiList = poiService.findAll()
//
//        poiList.forEach { poi ->
//            val newName = aiotService.parseDeviceId(poi.deviceId(), abbreviationList)
//            if (newName != poi.name()) {
//                poiService.updatePoiName(poi.id(), newName)
//            }
//        }
    }
}
