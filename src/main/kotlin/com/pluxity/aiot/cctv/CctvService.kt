package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvResponse
import com.pluxity.aiot.cctv.dto.toCctvResponse
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CctvService(
    private val cctvRepository: CctvRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(siteId: Long? = null): List<CctvResponse> {
        val list = cctvRepository.findAllBySiteId(siteId)
        return list.map { it.toCctvResponse() }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): CctvResponse = findById(id).toCctvResponse()

    fun findById(id: Long): Cctv =
        cctvRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_CCTV, id)
}
