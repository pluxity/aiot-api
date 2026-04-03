package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvResponse
import com.pluxity.aiot.cctv.dto.toCctvResponse
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.site.SiteRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CctvService(
    private val cctvRepository: CctvRepository,
    private val siteRepository: SiteRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(siteId: Long? = null): List<CctvResponse> {
        val list = cctvRepository.findAllBySiteId(siteId)
        return list.map { it.toCctvResponse() }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): CctvResponse = findById(id).toCctvResponse()

    @Transactional
    fun updateCoordinates(
        id: Long,
        lon: Double,
        lat: Double,
    ) {
        val cctv = findById(id)
        val site = siteRepository.findFirstByPointInPolygon(lon, lat)
        cctv.updateLocationInfo(lon, lat, site)
    }

    fun findById(id: Long): Cctv =
        cctvRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_CCTV, id)
}
