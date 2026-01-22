package com.pluxity.aiot.cctv

import com.pluxity.aiot.cctv.dto.CctvRequest
import com.pluxity.aiot.cctv.dto.CctvResponse
import com.pluxity.aiot.cctv.dto.toCctvResponse
import com.pluxity.aiot.cctv.repository.CctvRepository
import com.pluxity.aiot.global.constant.ErrorCode
import com.pluxity.aiot.global.exception.CustomException
import com.pluxity.aiot.global.properties.MediaMtxProperties
import com.pluxity.aiot.global.utils.UUIDUtils
import com.pluxity.aiot.site.SiteRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.map
import kotlin.let

private val log = KotlinLogging.logger {}

@Service
class CctvService(
    private val cctvRepository: CctvRepository,
    private val siteRepository: SiteRepository,
    private val mediaMtxService: MediaMtxService,
    private val mediaMtxProperties: MediaMtxProperties,
) {
    @PostConstruct
    fun init() {
//        try {
//            synchronizeCctv()
//        } catch (e: Exception) {
//            error { "CctvService 초기화 중 미디어서버 동기화 실패 (서버 상태를 확인하세요): ${e.message}" }
//        }
    }

    @Transactional
    fun create(request: CctvRequest): Long {
        val site =
            if (request.lon != null && request.lat != null) {
                siteRepository.findFirstByPointInPolygon(request.lon, request.lat)
            } else {
                null
            }
        var mtxName: String? = null
        request.url?.let {
            mtxName = UUIDUtils.generateShortUUID()
            mediaMtxService.addPath(mtxName, request.url)
        }
        return cctvRepository
            .save(
                Cctv(
                    name = request.name,
                    url = request.url,
                    latitude = request.lat,
                    longitude = request.lon,
                    height = request.height,
                    mtxName = mtxName,
                    site = site,
                ),
            ).requiredId
    }

    @Transactional(readOnly = true)
    fun findAll(facilityId: Long? = null): List<CctvResponse> {
        val list = cctvRepository.findAllBySiteId(facilityId)
        return list.map { it.toCctvResponse(mediaMtxProperties.viewUrl) }
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): CctvResponse = findById(id).toCctvResponse(mediaMtxProperties.viewUrl)

    @Transactional
    fun update(
        id: Long,
        request: CctvRequest,
    ) {
        val cctv = findById(id)
        if (cctv.url != request.url) {
            cctv.mtxName?.let {
                mediaMtxService.deletePath(it)
            }
            var mtxName: String? = null
            request.url?.let {
                mtxName = UUIDUtils.generateShortUUID()
                mediaMtxService.addPath(mtxName, request.url)
            }
            cctv.updateMtxName(mtxName)
        }
        cctv.updateLocationEmpty()
        if (request.lon != null && request.lat != null) {
            val site = siteRepository.findFirstByPointInPolygon(request.lon, request.lat)
            cctv.updateLocationInfo(request.lon, request.lat, site)
        }
        cctv.updateCctv(request.name, request.url, request.height)
    }

    @Transactional
    fun delete(id: Long) {
        val cctv = findById(id)
        cctv.mtxName?.let {
            mediaMtxService.deletePath(it)
        }
        cctvRepository.deleteById(id)
    }

    fun findById(id: Long): Cctv =
        cctvRepository.findByIdOrNull(id)
            ?: throw CustomException(ErrorCode.NOT_FOUND_CCTV, id)

    fun synchronizeCctv() {
        val mtxList = mediaMtxService.getAllPath().map { it.name }
        val cctvList = cctvRepository.findAll()
        val dbList = cctvList.mapNotNull { it.mtxName }

        mtxList.minus(dbList.toSet()).forEach {
            mediaMtxService.deletePath(it)
        }

        dbList.minus(mtxList.toSet()).forEach {
            mediaMtxService.addPath(it, cctvList.first { cctv -> cctv.mtxName == it }.url ?: return@forEach)
        }
    }
}
